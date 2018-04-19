package tarehart.rlbot.planning.cancellation

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.Duration


class BallPathDisruptionMeter(
        private val distanceThreshold: Double = 20.0) {

    private lateinit var trackingSlice: BallSlice

    fun isDisrupted(path: BallPath): Boolean {
        if (!::trackingSlice.isInitialized) {
            trackingSlice = path.getMotionAt(path.startPoint.time.plusSeconds(4.0)) ?: return false
            return false
        }

        if (Duration.between(path.startPoint.time, trackingSlice.time) < Duration.ofSeconds(2.0)) {
            path.getMotionAt(path.startPoint.time.plusSeconds(4.0))?.let {
                trackingSlice = it
            }
        }

        val currentSlice = path.getMotionAt(trackingSlice.time) ?: return false

        if (currentSlice.space.distance(trackingSlice.space) > distanceThreshold) {
            return true
        }

        return false
    }
}