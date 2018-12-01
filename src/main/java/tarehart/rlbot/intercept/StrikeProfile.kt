package tarehart.rlbot.intercept

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

data class StrikeProfile(
        /**
         * The amount of time between strike initiation and any dodge.
         */
        val hangTime: Double = 0.0,
        /**
         * The amount of speed potentially gained over the course of the strike's final stage (generally after driving over and lining up)
         */
        val speedBoost: Double = 0.0,
        /**
         * The amount of time spent speeding up during the final stage
         */
        val dodgeSeconds: Double = 0.0,

        val style: Style = Style.CHIP) {

    val strikeDuration: Duration
        get() = Duration.ofSeconds(hangTime + dodgeSeconds)

    val verticallyAccessible: (CarData, SpaceTime) -> Boolean
        get() {
            if (style == Style.CHIP) {
                return { _, st -> st.space.z <= AirTouchPlanner.MAX_CHIP_HIT }
            }
            if (style == Style.FLIP_HIT) {
                return { _, st -> AirTouchPlanner.isFlipHitAccessible(st.space.z) }
            }
            if (style == Style.AERIAL) {
                return AirTouchPlanner::isVerticallyAccessible
            }
            return AirTouchPlanner::isJumpHitAccessible
        }

    val isForward: Boolean = style != Style.SIDE_HIT && style != Style.DIAGONAL_HIT

    data class PostDodgeVelocity(val forwardMagnitude: Double, val sidewaysMagnitude: Double) {
        val speed = Vector2(forwardMagnitude, sidewaysMagnitude).magnitude()
    }

    /**
     * When we are doing either a side dodge or a diagonal dodge, the car will speed up in the forward and horizontal directions.
     */
    fun getPostDodgeVelocity(arrivalSpeed: Double): PostDodgeVelocity {

        // https://youtu.be/pX950bhGhJE?t=370
        val sidewaysImpulseMagnitude = ManeuverMath.DODGE_SPEED * (1 + 0.9 *  arrivalSpeed / AccelerationModel.SUPERSONIC_SPEED)

        val sidewaysComponent = if (isForward) 0.0 else sidewaysImpulseMagnitude
        val forwardComponent = if (style == Style.SIDE_HIT) 0.0 else ManeuverMath.DODGE_SPEED

        val tentativeFinalSpeed = Vector2(arrivalSpeed + forwardComponent, sidewaysComponent)
        val finalSpeed = tentativeFinalSpeed.magnitude()
        if (finalSpeed > AccelerationModel.SUPERSONIC_SPEED) {
            val scaledSpeed = tentativeFinalSpeed.scaledToMagnitude(AccelerationModel.SUPERSONIC_SPEED)
            return PostDodgeVelocity(scaledSpeed.x, scaledSpeed.y)
        }

        return PostDodgeVelocity(tentativeFinalSpeed.x, tentativeFinalSpeed.y)
    }

    enum class Style {
        CHIP,
        FLIP_HIT,
        SIDE_HIT,
        DIAGONAL_HIT,
        JUMP_HIT,
        AERIAL
    }
}
