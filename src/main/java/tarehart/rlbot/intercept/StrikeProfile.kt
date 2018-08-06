package tarehart.rlbot.intercept

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.time.Duration

data class StrikeProfile @JvmOverloads constructor(
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

    enum class Style {
        CHIP,
        FLIP_HIT,
        SIDE_HIT,
        DIAGONAL_HIT,
        JUMP_HIT,
        AERIAL
    }
}
