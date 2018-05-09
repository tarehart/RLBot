package tarehart.rlbot.input

/**
 * All values are in radians per second.
 */
class CarSpin(
        /**
         * Positive means tilting upward
         */
        val pitchRate: Double,
        /**
         * Positive means turning to the right
         */
        val yawRate: Double,
        /**
         * Positive means rolling to the right
         */
        val rollRate: Double)
