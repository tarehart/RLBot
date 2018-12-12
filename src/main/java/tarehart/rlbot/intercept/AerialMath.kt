package tarehart.rlbot.intercept

import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.*
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

object AerialMath {

    val JUMP_ASSIST_DURATION = .5
    val JUMP_ASSIST_ACCEL = 12

    // This is a constant that we tuned with some testing. No mathematical basis
    val TYPICAL_UPWARD_ACCEL = 2.5

    private const val MINIMUM_JUMP_HEIGHT = 2.0
    private const val JUMP_VELOCITY = 12.0
    private const val AERIAL_ANGULAR_ACCEL = OrientationSolver.ALPHA_MAX  // maximum aerial angular acceleration
    const val BOOST_ACCEL_IN_AIR = 20.0  // boost acceleration
    private const val ACCEL_NEEDED_THRESHOLD = 0.95

    /**
     * Taken from https://github.com/samuelpmish/RLUtilities/blob/master/RLUtilities/Maneuvers.py#L415-L421
     */
    fun isViableAerial(car: CarSlice, target: SpaceTime, modelJump: Boolean, secondsSinceJump: Double): Boolean {

        val courseResult = calculateAerialCourseCorrection(car, target, modelJump, secondsSinceJump)
        val accelNeeded = courseResult.averageAccelerationRequired

        return 0 <= accelNeeded && accelNeeded < ACCEL_NEEDED_THRESHOLD * BOOST_ACCEL_IN_AIR
    }

    fun calculateAerialTimeNeeded(correction: AerialCourseCorrection): Duration {
        val errorDistance = correction.targetError.magnitude()
        val bestTime = correction.timeNeededForTurn.seconds + Math.sqrt(2 * errorDistance / (ACCEL_NEEDED_THRESHOLD * BOOST_ACCEL_IN_AIR))
        return Duration.ofSeconds(bestTime)
    }

    /**
     * Determines the direction and magnitude of boost correction required.
     *
     * Also considers whether the car is on the ground and accounts for initial upward velocity from
     * a hypothetical jump.
     *
     * Taken from https://github.com/samuelpmish/RLUtilities/blob/master/RLUtilities/Maneuvers.py#L423-L445
     */
    fun calculateAerialCourseCorrection(car: CarSlice, target: SpaceTime, modelJump: Boolean, secondsSinceJump: Double): AerialCourseCorrection {

        var initialPosition = car.space
        var initialVelocity = car.velocity

        val secondsRemaining = (target.time - car.time).seconds

        if (modelJump) {
            initialVelocity += car.orientation.roofVector * JUMP_VELOCITY
            initialPosition += car.orientation.roofVector * MINIMUM_JUMP_HEIGHT
        }

        val assistSeconds = Math.max(0.0, JUMP_ASSIST_DURATION - secondsSinceJump)

        val positionAfterJumpAssist = initialPosition + initialVelocity * assistSeconds -
                Vector3.UP * 0.5 * (ArenaModel.GRAVITY - JUMP_ASSIST_ACCEL) * assistSeconds * assistSeconds

        val postAssistSeconds = secondsRemaining - assistSeconds
        val velocityAfterJumpAssist = initialVelocity + Vector3.UP * assistSeconds * (JUMP_ASSIST_ACCEL - ArenaModel.GRAVITY)

        val expectedCarPosition = positionAfterJumpAssist + velocityAfterJumpAssist * postAssistSeconds -
                Vector3.UP * 0.5 * ArenaModel.GRAVITY * postAssistSeconds * postAssistSeconds

        // Displacement from where the car will end up to where we're aiming
        val targetError = target.space - expectedCarPosition

        val correctionDirection = targetError.normaliseCopy()

        // estimate the time required to turn
        val angleToTurn = car.orientation.matrix.angleTo(Mat3.lookingTo(correctionDirection, car.orientation.roofVector))
        val secondsNeededForTurn = 0.85 * (2.0 * Math.sqrt(angleToTurn / AERIAL_ANGULAR_ACCEL))

        // see if the boost acceleration needed to reach the target is achievable
        // Assume that we will only be boosting when the car is lined up with the required orientation.
        val errorDistance = targetError.magnitude()
        val averageAccelerationRequired = 2.0 * errorDistance / Math.pow(secondsRemaining - secondsNeededForTurn, 2.0)

        return AerialCourseCorrection(correctionDirection, averageAccelerationRequired, targetError, Duration.ofSeconds(secondsNeededForTurn))
    }

    fun timeToAir(height: Double): Double {
        val a = TYPICAL_UPWARD_ACCEL // Upward acceleration
        val b = 10 // Initial upward velocity from jump
        val c = -(height - ManeuverMath.BASE_CAR_Z)
        val liftoffDelay = 0.3
        return (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a) + liftoffDelay
    }

}
