package tarehart.rlbot.planning.cancellation

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.time.Duration


class InterceptDisruptionMeter(
        private val distanceThreshold: Double = 30.0,
        private val timeThreshold: Duration? = null) {

    private lateinit var originalIntercept: BallSlice

    fun isDisrupted(intercept: BallSlice): Boolean {
        if (!::originalIntercept.isInitialized) {
            originalIntercept = intercept
            return false
        }

        if (intercept.space.distance(originalIntercept.space) > distanceThreshold) {
            return true
        }

        timeThreshold ?.let {
            if (Duration.between(intercept.time, originalIntercept.time).abs() > it) {
                return true
            }
        }

        return false
    }
}