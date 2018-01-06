package tarehart.rlbot.intercept

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

    enum class Style {
        RAM,
        FLIP_HIT,
        SIDE_HIT,
        JUMP_HIT,
        AERIAL
    }
}
