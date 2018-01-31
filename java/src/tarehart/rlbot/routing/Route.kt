package tarehart.rlbot.routing

import tarehart.rlbot.time.GameTime
import java.awt.Graphics2D

class Route(val anchorTime: GameTime, val workBackwards: Boolean = false) {

    val parts = ArrayList<RoutePart>()

    fun withPart(part: RoutePart): Route {
        parts.add(part)
        return this
    }

    fun drawDebugInfo(graphics: Graphics2D) {
        parts.forEach { it.drawDebugInfo(graphics) }
    }

}