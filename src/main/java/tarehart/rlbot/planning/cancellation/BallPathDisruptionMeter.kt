package tarehart.rlbot.planning.cancellation

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.Duration


class BallPathDisruptionMeter(
        private val distanceThreshold: Number = 5.0) {

    private var trackingSlice: BallSlice? = null

    fun isDisrupted(path: BallPath): Boolean {

        if (trackingSlice == null) {
            trackingSlice = path.getMotionAt(path.startPoint.time.plusSeconds(4.0)) ?: return false
            return false
        }

        val oldSlice = trackingSlice ?: return false
        val currentSlice = path.getMotionAt(oldSlice.time) ?: return false

        if (currentSlice.space.distance(oldSlice.space) > distanceThreshold.toDouble()) {
            return true
        }

        if (Duration.between(path.startPoint.time, oldSlice.time) < Duration.ofSeconds(3.5)) {
            path.getMotionAt(path.startPoint.time.plusSeconds(4.0))?.let {
                trackingSlice = it
            }
        }

        return false
    }

    fun reset() {
        trackingSlice = null
    }
}
