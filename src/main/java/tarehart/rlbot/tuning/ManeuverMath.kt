package tarehart.rlbot.tuning

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.routing.DistanceDuration
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.time.Duration
import kotlin.math.sqrt

object ManeuverMath {

    const val DODGE_SPEED = 10.0F

    const val BASE_CAR_Z = 0.3405F

    const val MASH_JUMP_HEIGHT = 4.8F // This is absolute height, so subtract BASE_CAR_Z if you want relative height.

    const val BRAKING_DECELERATION = 30.0F

    private const val TAP_JUMP_HEIGHT = 1.88F - BASE_CAR_Z
    private const val TAP_JUMP_APEX_TIME = 0.5F


    private const val MASH_A = -6.451144F
    private const val MASH_B = 11.26689F
    private const val MASH_C = -0.09036379F
    private const val MASH_SLOPE = 7.78F
    private const val SLOPE_CUTOFF = 3.2F

    /**
     * @param height the raw height that we want the center of the car to achieve.
     * If the height is less than BASE_CAR_Z, there's nothing to do.
     */
    fun secondsForMashJumpHeight(height: Float): Float? {
        if (height > MASH_JUMP_HEIGHT) {
            return null
        }

        if (height < SLOPE_CUTOFF) {
            return (height - BASE_CAR_Z) / MASH_SLOPE
        }

        val d = MASH_B * MASH_B - 4F * MASH_A * (MASH_C - height)
        return if (d < 0) {
            null // Too high!
        } else (-MASH_B + sqrt(d)) / (2 * MASH_A)
    }

    fun secondsForSideFlipTravel(distance: Double): Double {
        return distance / DODGE_SPEED
    }

    fun isSkidding(car: CarData): Boolean {
        return !car.velocity.isZero && car.velocity.normaliseCopy().dotProduct(car.orientation.noseVector) < .98
    }

    fun isOnGround(car: CarData): Boolean {
        return car.hasWheelContact &&
                car.orientation.roofVector.dotProduct(Vector3(0.0, 0.0, 1.0)) > .98 &&
                car.position.z < BASE_CAR_Z + 0.2
    }

    fun forwardSpeed(car: CarData): Float {
        return car.velocity.dotProduct(car.orientation.noseVector)
    }

    fun getBrakeDistance(speed: Float): Float {
        // TODO: make this incorporate BRAKING_DECELERATION, and make it accurate
        return speed * speed * .01F + speed * .1F
    }

    /**
     * Does it look like the car is lined up, but on the wrong side of the launchpad?
     */
    fun hasBlownPast(car: CarData, position: Vector2, facing: Vector2): Boolean {
        val toPad = position - car.position.flatten()
        val approachError = Vector2.angle(toPad, facing)
        val orientationError = Vector2.angle(car.orientation.noseVector.flatten(), facing)

        if (orientationError > Math.PI / 12) {
            return false // Never lined up to begin with
        }

        if (approachError < Math.PI / 12) {
            return false // haven't blown past yet, still looking good
        }

        return toPad.magnitude() < 2 || approachError > Math.PI * 11 / 12
    }

    fun hasBlownPast(car: CarData, position: Vector2): Boolean {
        val toPad = position - car.position.flatten()
        val orientationError = Vector2.angle(car.orientation.noseVector.flatten(), toPad)
        return orientationError > Math.PI * .95 && toPad.magnitude() < 2
    }

    fun estimateApproachVector(currentFacing: PositionFacing, target: Vector2): Vector2 {
        // When we are close to the target, the current orientation matters, and is probably taking some offset
        // into account if we have been approaching few frames, so currentFacing
        // is most accurate.

        // When we are far from the target, we should bias for the approach angle because it guards us against the
        // situation where we just found out about the circle and the car is pointed the completely wrong way. Also
        // the approach angle is a pretty good approximation when we are far away.

        // This summation leads to a weighted average of the two vectors depending on distance.

        val toTarget = target - currentFacing.position
        if (toTarget.isZero) {
            return currentFacing.facing
        }
        val estimatedEntryAngle = currentFacing.facing + toTarget.scaledToMagnitude(2 + toTarget.magnitude())
        return estimatedEntryAngle.normalized()
    }

    fun getDecelerationDistanceWhenTargetingSpeed(start: Vector2, end: Vector2, desiredSpeed: Float, distancePlot: DistancePlot): DistanceDuration {

        val distance = start.distance(end)
        // Assume we'll only accelerate half way.
        val maxAccelMotion = distancePlot.getMotionAfterDistance(distance / 2) ?: return DistanceDuration(0F, Duration.ofMillis(0))

        if (maxAccelMotion.speed <= desiredSpeed) {
            return DistanceDuration(0F, Duration.ofMillis(0))
        }

        val speedDiff = maxAccelMotion.speed - desiredSpeed
        val secondsDecelerating = speedDiff / ManeuverMath.BRAKING_DECELERATION
        val avgSpeed = (maxAccelMotion.speed + desiredSpeed) / 2
        return DistanceDuration(avgSpeed * secondsDecelerating, Duration.ofSeconds(secondsDecelerating))
    }

    fun getMotionAfterSpeedChange(currentSpeed: Float, idealSpeed: Float, forwardAccelPlot: DistancePlot): DistanceTimeSpeed? {

        if (idealSpeed < currentSpeed) {
            val secondsRequired = (currentSpeed - idealSpeed) / ManeuverMath.BRAKING_DECELERATION
            val avgSpeed = (currentSpeed + idealSpeed) / 2
            return DistanceTimeSpeed(
                    avgSpeed * secondsRequired,
                    Duration.ofSeconds(secondsRequired),
                    idealSpeed)
        }

        return forwardAccelPlot.getMotionUponSpeed(idealSpeed)
    }

}
