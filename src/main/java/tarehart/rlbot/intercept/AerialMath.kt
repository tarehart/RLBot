package tarehart.rlbot.intercept

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.*
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

object AerialMath {

    val EFFECTIVE_AIR_BOOST_ACCELERATION = 18.0
    val JUMP_ASSIST_DURATION = .6
    val JUMP_ASSIST = 12
    val TYPICAL_UPWARD_ACCEL = (EFFECTIVE_AIR_BOOST_ACCELERATION - ArenaModel.GRAVITY) * .5 // Assume a 30 degree upward angle

    private const val MINIMUM_JUMP_HEIGHT = 2.0
    private const val JUMP_VELOCITY = 12.0
    private const val AERIAL_ANGULAR_ACCEL = OrientationSolver.ALPHA_MAX  // maximum aerial angular acceleration
    const val BOOST_ACCEL_IN_AIR = 20.0  // boost acceleration

    /**
     * Taken from https://github.com/samuelpmish/RLUtilities/blob/master/RLUtilities/Maneuvers.py#L415-L421
     */
    fun isViableAerial(car: CarData, target: SpaceTime): Boolean {

        val courseResult = calculateAerialCourseCorrection(car, target)
        val boostFeathering = courseResult.averageAccelerationRequired

        return 0 <= boostFeathering && boostFeathering < 0.95 * BOOST_ACCEL_IN_AIR

    }

    /**
     * Determines the direction and magnitude of boost correction required.
     *
     * Also considers whether the car is on the ground and accounts for initial upward velocity from
     * a hypothetical jump.
     *
     * Taken from https://github.com/samuelpmish/RLUtilities/blob/master/RLUtilities/Maneuvers.py#L423-L445
     */
    fun calculateAerialCourseCorrection(car: CarData, target: SpaceTime): AerialCourseCorrection {
        var initialPosition = car.position
        var initialVelocity = car.velocity

        val secondsRemaining = (target.time - car.time).seconds

        if (car.hasWheelContact) {
            initialVelocity += car.orientation.roofVector * JUMP_VELOCITY
            initialPosition += car.orientation.roofVector * MINIMUM_JUMP_HEIGHT
        }

        val expectedCarPosition = initialPosition + initialVelocity * secondsRemaining - Vector3.UP * 0.5 * ArenaModel.GRAVITY * secondsRemaining * secondsRemaining

        // Displacement from where the car will end up to where we're aiming
        val targetError = target.space - expectedCarPosition

        RenderUtil.drawSphere(car.renderer, expectedCarPosition, 2.0, Color.RED)

        val correctionDirection = targetError.normaliseCopy()

        // estimate the time required to turn
        val angleToTurn = car.orientation.matrix.angleTo(Mat3.lookingTo(correctionDirection, car.orientation.roofVector))
        val secondsNeededForTurn = 0.7 * (2.0 * Math.sqrt(angleToTurn / AERIAL_ANGULAR_ACCEL))

        // see if the boost acceleration needed to reach the target is achievable
        // Assume that we will only be boosting when the car is lined up with the required orientation.
        val averageAccelerationRequired = 2.0 * targetError.magnitude() / Math.pow(secondsRemaining - secondsNeededForTurn, 2.0)

        return AerialCourseCorrection(correctionDirection, averageAccelerationRequired, targetError)
    }

    fun getDesiredZComponentBasedOnAccel(targetHeight: Double, timeTillIntercept: Duration, timeSinceLaunch: Duration, car: CarData): Double {

        val additionalHeight = targetHeight - car.position.z
        val secondsTillIntercept = timeTillIntercept.seconds
        val averageVelNeeded = additionalHeight / secondsTillIntercept
        val initialVel = car.velocity.z
        val finalVel = initialVel + (averageVelNeeded - initialVel) * 2
        val averageAccel = (finalVel - initialVel) / secondsTillIntercept

        val secondsSinceLaunch = timeSinceLaunch.seconds
        val jumpAssistSecondsRemaining = Math.max(0.0, JUMP_ASSIST_DURATION - secondsSinceLaunch)
        val proportionAssistedByJump = jumpAssistSecondsRemaining / (jumpAssistSecondsRemaining + secondsTillIntercept)

        val boostAccelToComplementJumpAssist = averageAccel - (JUMP_ASSIST - ArenaModel.GRAVITY)
        val boostAccelNeededPostJump = averageAccel + ArenaModel.GRAVITY

        val averageBoostAccelNeeded = boostAccelToComplementJumpAssist * proportionAssistedByJump + boostAccelNeededPostJump * (1 - proportionAssistedByJump)

        return Clamper.clamp(averageBoostAccelNeeded / EFFECTIVE_AIR_BOOST_ACCELERATION, -1.0, 1.0)
    }

    // TODO: add some of this jump assist logic to chips aerial math.
    fun getProjectedHeight(car: CarData, secondsTillIntercept: Double, secondsSinceLaunch: Double): Double {

        val initialHeight = car.position.z
        val initialVelocity = car.velocity.z
        val noseVertical = car.orientation.noseVector.z

        val phase2Height: Double
        val phase2Velocity: Double

        val tjmp = Math.max(0.0, JUMP_ASSIST_DURATION - secondsSinceLaunch)
        if (tjmp > 0) {
            val verticalAcceleration = EFFECTIVE_AIR_BOOST_ACCELERATION * noseVertical + JUMP_ASSIST - ArenaModel.GRAVITY
            phase2Height = initialHeight + initialVelocity * tjmp + .5 * verticalAcceleration * tjmp * tjmp
            phase2Velocity = initialVelocity + verticalAcceleration * tjmp
        } else {
            phase2Height = initialHeight
            phase2Velocity = initialVelocity
        }

        val tfly = secondsTillIntercept - tjmp

        val verticalAcceleration = EFFECTIVE_AIR_BOOST_ACCELERATION * noseVertical - ArenaModel.GRAVITY

        return phase2Height + phase2Velocity * tfly + .5 * verticalAcceleration * tfly * tfly
    }

    fun timeToAir(height: Double): Double {
        val a = TYPICAL_UPWARD_ACCEL // Upward acceleration
        val b = 10 // Initial upward velocity from jump
        val c = -(height - ManeuverMath.BASE_CAR_Z)
        val liftoffDelay = 0.5
        return (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a) + liftoffDelay
    }

}
