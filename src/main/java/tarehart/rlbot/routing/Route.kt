package tarehart.rlbot.routing

import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Graphics2D

class Route(val anchorTime: GameTime, val workBackwards: Boolean = false) {

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