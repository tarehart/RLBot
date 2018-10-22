package tarehart.rlbot.math

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Clamper.clamp
import tarehart.rlbot.math.vector.Spin
import tarehart.rlbot.math.vector.Vector3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sin


object OrientationSolver {

    val ALPHA_MAX = 9.0

    val ROLL_TORQUE = -36.07956616966136
    val PITCH_TORQUE = -12.14599781908070
    val YAW_TORQUE =   8.91962804287785

    val ROLL_DRAG =  -4.47166302201591
    val PITCH_DRAG = -2.798194258050845
    val YAW_DRAG = -1.886491900437232

    val MOMENT_OF_INERTIA = 10.5

    /**
     * I don't understand the point of this yet:
     * http://www.wolframalpha.com/input/?i=y+%3D+((x+-+pi)+mod+(2+*+pi))+%2B+pi
     *
     * It clamps to a domain of PI to 3PI, which does not seem very useful
     */
    fun periodic(x: Double): Double {
        return ((x - Math.PI) % (2 * Math.PI)) + Math.PI
    }

    fun q(x: Double): Double {
        return 1.0 - (1.0 / (1.0 + 500.0 * x * x))
    }

    fun r(delta: Double, v: Double): Double {
        return delta - 0.5 * Math.signum(v) * v * v / ALPHA_MAX
    }

    fun getNecessaryAccel(relativeRadians: Double, currentAngularVel: Double, dt: Double): Double {
        val ri = r(relativeRadians, currentAngularVel)
        val alpha = Math.signum(ri) * ALPHA_MAX
        val rf = r(relativeRadians - currentAngularVel * dt, currentAngularVel + alpha * dt)

        // use a single step of secant method to improve
        // the acceleration when residual changes sign
        if (ri * rf < 0.0) {
            return alpha * (2.0 * (ri / (ri - rf)) - 1)
        }

        return alpha
    }

    fun aerialSpin(initialSpin: Vector3, targetSpin: Vector3, initialOrientation: Mat3, dt: Double): Spin {
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

        expectedDamping[0] = 0.0

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
    fun solvePiecewiseLinear(accelConstant: Double, dampingConstant: Double, desiredChange: Double): Double {
        val decelDivisor = accelConstant + dampingConstant
        val accelDivisor = accelConstant - dampingConstant

        val deceleratingMagnitude = if (abs(decelDivisor) > 10e-6) desiredChange / decelDivisor else -1.0
        val acceleratingMagnitude = if (abs(accelDivisor) > 10e-6) desiredChange / accelDivisor else 1.0

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

        return 0.0
    }

    /*

    https://pastebin.com/XtFL5JzV

    def step(self, dt):

        relative_rotation = dot(transpose(self.car.theta), self.target)
        geodesic_local = rotation_to_axis(relative_rotation)

        # figure out the axis of minimal rotation to target
        geodesic_world = dot(self.car.theta, geodesic_local)

        # get the angular acceleration
        alpha = vec3(
            self.controller(geodesic_world[0], self.car.omega[0], dt),
            self.controller(geodesic_world[1], self.car.omega[1], dt),
            self.controller(geodesic_world[2], self.car.omega[2], dt)
        )

        # reduce the corrections for when the solution is nearly converged
        for i in range(0, 3):
            error = abs(geodesic_world[i]) + abs(self.car.omega[i]);
            alpha[i] = self.q(error) * alpha[i]

        # set the desired next angular velocity
        omega_next = self.car.omega + alpha * dt

        # determine the controls that produce that angular velocity
        roll_pitch_yaw = aerial_rpy(self.car.omega, omega_next, self.car.theta, dt)

        self.controls.roll  = roll_pitch_yaw[0]
        self.controls.pitch = roll_pitch_yaw[1]
        self.controls.yaw   = roll_pitch_yaw[2]

     */
    fun step(car: CarData, targetOrientation: Mat3, dt: Double): AgentOutput {
        val relativeRotation = car.orientation.matrix.transpose().dot(targetOrientation)
        val geodesicLocal = rotationToAxis(relativeRotation)

        val geodesicWorld = car.orientation.matrix.dot(geodesicLocal)

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

        val spin = aerialSpin(angularVel, desiredNextAngularVel, car.orientation.matrix, dt)

        return AgentOutput()
                .withPitch(spin.pitchRate)
                .withYaw(spin.yawRate)
                .withRoll(spin.rollRate)
    }

    fun errorCorrect(geoWorldComponent: Double, angularVelComponent:Double, angularAccelComponent:Double): Double {
        val error = abs(geoWorldComponent) + abs(angularVelComponent)
        return q(error) * angularAccelComponent
    }

    /*
        https://github.com/samuelpmish/RLUtilities/blob/master/RLUtilities/cpp/inc/linalg.h#L111

        inline vec < 3 > rotation_to_axis(const mat < 3, 3 > & R) {

          float theta = acos(clamp(0.5f * (tr(R) - 1.0f), -1.0f, 1.0f));

          float scale;

          // for small angles, prefer series expansion to division by sin(theta) ~ 0
          if (fabs(theta) < 0.00001f) {
            scale = 0.5f + theta * theta / 12.0f;
          } else {
            scale = 0.5f * theta / sin(theta);
          }

          return vec3{R(2,1)-R(1,2), R(0,2)-R(2,0), R(1,0)-R(0,1)} * scale;

        }
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

    fun orientationLookingInDirection(direction: Vector3): Mat3 {
        val f = direction.normaliseCopy()
        val u = f.crossProduct(Vector3.UP.crossProduct(f)).normaliseCopy()
        val l = Vector3.UP.crossProduct(f).normaliseCopy()

        return Mat3(arrayOf(doubleArrayOf(f.x, l.x, u.x), doubleArrayOf(f.y, l.y, u.y), doubleArrayOf(f.z, l.z, u.z)))
    }

}
