package tarehart.rlbot.planning.cancellation

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.Duration


class BallPathDisruptionMeter(
        private val distanceThreshold: Number = 20.0) {

    private var trackingSlice: BallSlice? = null

    fun isDisrupted(path: BallPath): Boolean {

        if (trackingSlice == null) {
            trackingSlice = path.getMotionAt(path.startPoint.time.plusSeconds(1.0)) ?: return false
            return false
        }

        var slice = trackingSlice ?: return false

        if (Duration.between(path.startPoint.time, slice.time) < Duration.ofSeconds(0.5)) {
            path.getMotionAt(path.startPoint.time.plusSeconds(1.0))?.let {
                trackingSlice = it
                slice = it
            }
        }

        val currentSlice = path.getMotionAt(slice.time) ?: return false

        if (currentSlice.space.distance(slice.space) > distanceThreshold.toDouble()) {
            return true
        }

        return false
    }

    fun reset() {
        trackingSlice = null
    }
}
