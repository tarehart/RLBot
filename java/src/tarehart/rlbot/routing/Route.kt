package tarehart.rlbot.routing

import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Graphics2D

/**
 * Route should represent the minimum time required to satisfy position and velocity constraints,
 * even if we're actually going to go at a leisurely pace, e.g. to wait for the ball.
 */
class Route {

    val parts = ArrayList<RoutePart>()
    var duration = Duration.ofMillis(0)

    fun withPart(part: RoutePart): Route {
        parts.add(part)
        duration += part.duration
        return this
    }

    fun drawDebugInfo(graphics: Graphics2D) {
        parts.forEach { it.drawDebugInfo(graphics) }
    }

}