package tarehart.rlbot.intercept

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.time.Duration

data class StrikeProfile @JvmOverloads constructor(
        /**
         * the extra approach time added by final maneuvers before striking the ball
         */
        val travelDelay: Double = 0.0,
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

        val style: Style = Style.RAM) {

    val strikeDuration: Duration
        get() = Duration.ofSeconds(hangTime + dodgeSeconds)

    val verticallyAccessible: (CarData, SpaceTime) -> Boolean
        get() {
            if (style == Style.RAM) {
                return { _, st -> st.space.z < ArenaModel.BALL_RADIUS + .3 }
            }
            if (style == Style.FLIP_HIT) {
                return { _, st -> AirTouchPlanner.isFlipHitAccessible(st.space) }
            }
            if (style == Style.AERIAL) {
                return AirTouchPlanner::isVerticallyAccessible
            }
            return AirTouchPlanner::isJumpHitAccessible
        }

    enum class Style {
        RAM,
        FLIP_HIT,
        SIDE_HIT,
        DIAGONAL_HIT,
        JUMP_HIT,
        AERIAL
    }
}
