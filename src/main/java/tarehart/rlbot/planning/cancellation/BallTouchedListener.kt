package tarehart.rlbot.planning.cancellation

import tarehart.rlbot.input.BallTouch


class BallTouchedListener {

    private var trackingTouch: BallTouch? = null

    fun hasNewTouch(touch: BallTouch?): Boolean {

        if (trackingTouch == null) {
            trackingTouch = touch ?: return false
            return false
        }

        trackingTouch?.let {
            if (touch != null && touch.time != it.time) {
                return true
            }
        }

        return false
    }

    fun reset() {
        trackingTouch = null
    }
}
