package tarehart.rlbot.planning.cancellation

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.time.Duration


class InterceptDisruptionMeter(
        private val distanceThreshold: Double = 30.0,
        private val timeThreshold: Duration? = null) {

    private var originalIntercept: BallSlice? = null

    fun isDisrupted(intercept: BallSlice): Boolean {

        val orig = originalIntercept

        if (orig == null) {
            originalIntercept = intercept
            return false
        }

        if (intercept.space.distance(orig.space) > distanceThreshold) {
            return true
        }

        timeThreshold ?.let {
            if (Duration.between(intercept.time, orig.time).abs() > it) {
                return true
            }
        }

        return false
    }

    fun reset(newIntercept: BallSlice?) {
        originalIntercept = newIntercept
    }
}
