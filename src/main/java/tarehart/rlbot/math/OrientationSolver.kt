package tarehart.rlbot.math

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Clamper.clamp
import tarehart.rlbot.math.vector.Spin
import tarehart.rlbot.math.vector.Vector3
import kotlin.math.*


/**
 * TODO: This logic has been improved upon in chip's repo. Try to port over the improvements.
 * https://github.com/samuelpmish/RLUtilities/blob/c5f9d7df95a7f62191bc652328bcc00d7753e629/main/cpp/src/mechanics/AerialTurn.cpp
 */
object OrientationSolver {

    const val ALPHA_MAX = 9.0F

    private const val MOMENT_OF_INERTIA = 10.5F

    private fun q(x: Float): Float {
        return 1 - (1 / (1 + 500 * x * x))
    }

    private fun r(delta: Float, v: Float): Float {
        return delta - 0.5F * sign(v) * v * v / ALPHA_MAX
    }

    private fun getNecessaryAccel(relativeRadians: Float, currentAngularVel: Float, dt: Float): Float {
        val ri = r(relativeRadians, currentAngularVel)
        val alpha = sign(ri) * ALPHA_MAX
        val rf = r(relativeRadians - currentAngularVel * dt, currentAngularVel + alpha * dt)

        // use a single step of secant method to improve
        // the acceleration when residual changes sign
        if (ri * rf < 0.0) {
            return alpha * (2 * (ri / (ri - rf)) - 1)
        }

        return alpha
    }

    private fun aerialSpin(initialSpin: Vector3, targetSpin: Vector3, initialOrientation: Mat3, dt: Float): Spin {
        // car's moment of inertia (spherical symmetry)


        // aerial control torque coefficients. 400 means that when you try to spin around the x axis,
        // which means rolling because the x axis comes out the front of the car, you will have 400 torque
        // available to you.
        // roll, pitch, yaw
        val torque = Vector3(400.0, 130.0, 95.0)

        // aerial damping torque coefficients
        val damping = Vector3(-50.0, -30.0, -20.0)

        val initialLocal = initialOrientation.transpose().dot(initialSpin)
        val targetLocal = initialOrientation.transpose().dot(targetSpin)

        val possibleAccel = (0..2).map { torque[it] * dt / MOMENT_OF_INERTIA }
        val expectedDamping = (0..2).map { -initialLocal[it] * damping[it] * dt / MOMENT_OF_INERTIA }.toMutableList()
        val toTarget = (0..2).map { targetLocal[it] - (1 + damping[it] * dt / MOMENT_OF_INERTIA) * initialLocal[it] }

        expectedDamping[0] = 0F

        return Spin(Vector3(
                solvePiecewiseLinear(possibleAccel[0], expectedDamping[0], toTarget[0]),
                solvePiecewiseLinear(possibleAccel[1], expectedDamping[1], toTarget[1]),
                solvePiecewiseLinear(possibleAccel[2], expectedDamping[2], toTarget[2])))

    }

    /**
     * We're trying to decide the magnitude to apply to one of our controls, e.g. pitch.
     * We want to achieve a particular change in the angular velocity.
     *
     * This function takes into account the concept that it's easier to decrease that velocity
     * if you get assisted by damping torque, as opposed to fighting it. Damping torque is weird, btw.
     *
     * Solves a piecewise linear (PWL) equation of the form
     *  a x + b | x | + (or - ?) c == 0
     *  https://www.wolframalpha.com/input/?i=a*x+%2B+b*abs(x)+%2B+c+%3D+0
     *
     * for -1 <= x <= 1. If no solution exists, this returns
     * the x value that gets closest
     *
     * Taken from
     * https://github.com/samuelpmish/RLUtilities/blob/d8360844e07afa32bff9b3c039022eb67cb82b33/RLUtilities/Maneuvers.py#L122
     */
    private fun solvePiecewiseLinear(accelConstant: Float, dampingConstant: Float, desiredChange: Float): Float {
        val decelDivisor = accelConstant + dampingConstant
        val accelDivisor = accelConstant - dampingConstant

        val deceleratingMagnitude = if (abs(decelDivisor) > 10e-6) desiredChange / decelDivisor else -1F
        val acceleratingMagnitude = if (abs(accelDivisor) > 10e-6) desiredChange / accelDivisor else 1F

        if (acceleratingMagnitude <= 0 && deceleratingMagnitude >= 0) {
            // This means our angular velocity will change in the right direction regardless of how
            // we try to control the car. Go with the easier magnitude. TODO: when does this happen?
            return if (abs(deceleratingMagnitude) < abs(acceleratingMagnitude)) {
                clamp(deceleratingMagnitude, 0.0, 1.0)
            } else {
                clamp(acceleratingMagnitude, -1.0, 0.0)
            }
        } else {
            // In this case, we actually have the power to move the car in the wrong direction.
            // Choose the right direction
            if (deceleratingMagnitude >= 0) {
                return clamp(deceleratingMagnitude, 0.0, 1.0)
            }
            if (acceleratingMagnitude <= 0) {
                return clamp(acceleratingMagnitude, -1.0, 0.0)
            }
        }

        return 0F
    }



    /**
     * https://github.com/samuelpmish/RLUtilities/blob/27807c2bff64ffdd89ddc943e2aa10d9fbb56901/RLUtilities/Maneuvers.py#L253
     */
    fun orientCar(car: CarData, targetOrientation: Mat3, dt: Float): AgentOutput {


        // Our data about the car lags behind a bit. Extrapolate forward to get a more realistic orientation.
        // Taken from https://github.com/samuelpmish/RLUtilities/blob/27807c2bff64ffdd89ddc943e2aa10d9fbb56901/RLUtilities/cpp/src/Car.cpp#L94-L101
        val extrapolatedOrientation = axisToRotation(car.spin.angularVelGlobal * dt).dot(car.orientation.matrix)

//        val currentTip = (car.position + car.orientation.noseVector.scaledToMagnitude(5.0))
//        val futureTip = (car.position + extrapolatedOrientation.forward().scaledToMagnitude(5.0))
//        car.renderer.drawLine3d(Color.WHITE,
//                car.position.toRlbot(),
//                currentTip.toRlbot())
//        car.renderer.drawLine3d(Color.WHITE,
//                currentTip.toRlbot(),
//                (currentTip + car.orientation.roofVector).toRlbot())
//
//        car.renderer.drawLine3d(Color.GREEN,
//                car.position.toRlbot(),
//                futureTip.toRlbot())
//        car.renderer.drawLine3d(Color.GREEN,
//                futureTip.toRlbot(),
//                (futureTip + extrapolatedOrientation.up()).toRlbot())
//        car.renderer.drawLine3d(Color.WHITE, car.position.toRlbot(), (car.position + car.spin.angularVelGlobal).toRlbot())

        val relativeRotation = extrapolatedOrientation.transpose().dot(targetOrientation)
        val geodesicLocal = rotationToAxis(relativeRotation)

        val geodesicWorld = extrapolatedOrientation.dot(geodesicLocal)

        val angularVel = car.spin.angularVelGlobal

        val angularAccel = Vector3(
                getNecessaryAccel(geodesicWorld.x, angularVel.x, dt),
                getNecessaryAccel(geodesicWorld.y, angularVel.y, dt),
                getNecessaryAccel(geodesicWorld.z, angularVel.z, dt))


        val angularAccelCorrected = Vector3(
                errorCorrect(geodesicWorld.x, angularVel.x, angularAccel.x),
                errorCorrect(geodesicWorld.y, angularVel.y, angularAccel.y),
                errorCorrect(geodesicWorld.z, angularVel.z, angularAccel.z)
        )

        val desiredNextAngularVel = angularVel + angularAccelCorrected * dt

        val spin = aerialSpin(angularVel, desiredNextAngularVel, extrapolatedOrientation, dt)

        return AgentOutput()
                .withPitch(spin.pitchRate)
                .withYaw(spin.yawRate)
                .withRoll(spin.rollRate)
    }

    private fun errorCorrect(geoWorldComponent: Float, angularVelComponent:Float, angularAccelComponent:Float): Float {
        val error = abs(geoWorldComponent) + abs(angularVelComponent)
        return q(error) * angularAccelComponent
    }

    /**
     * Takes a rotation matrix and finds the axis of rotation (with appropriate magnitude)
     * https://github.com/samuelpmish/RLUtilities/blob/f071b4dec24d3389f21727ca2d95b75980cbb5fb/RLUtilities/cpp/inc/linalg.h#L111
     */
    private fun rotationToAxis(rotation: Mat3): Vector3 {
        val theta = acos(clamp(0.5 * (rotation.trace() - 1), -1.0, 1.0))

        val scale = if (abs(theta) < 0.00001) {
            0.5 + theta * theta / 12.0
        } else {
            0.5 * theta / sin(theta)
        }

        return Vector3(
                rotation.get(2, 1) - rotation.get(1, 2),
                rotation.get(0, 2) - rotation.get(2, 0),
                rotation.get(1, 0) - rotation.get(0, 1)) * scale
    }

    /**
     * Takes a vector representing an axis of rotation and turns it into a rotation matrix.
     * https://github.com/samuelpmish/RLUtilities/blob/f071b4dec24d3389f21727ca2d95b75980cbb5fb/RLUtilities/cpp/inc/linalg.h#L87
     */
    private fun axisToRotation(axis: Vector3): Mat3 {
        val rotationAmount = axis.magnitude()
        if (abs(rotationAmount) < 0.000001) {
            return Mat3.IDENTITY
        }

        val axisNorm = axis.normaliseCopy()

        val k = Mat3(arrayOf(
                floatArrayOf(0F, -axisNorm.z, axisNorm.y),
                floatArrayOf(axisNorm.z, 0F, -axisNorm.x),
                floatArrayOf(-axisNorm.y, axisNorm.x, 0F)
        ))

        return Mat3.IDENTITY + k * sin(rotationAmount) + k.dot(k) * (1 - cos(rotationAmount))
    }
}
