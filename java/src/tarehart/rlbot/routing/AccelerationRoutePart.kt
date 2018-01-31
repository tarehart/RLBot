package tarehart.rlbot.routing

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.Duration
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D


class AccelerationRoutePart(
        override val start: Vector2,
        override val end: Vector2,
        override val duration: Duration) : RoutePart {

    override val waypoint: Vector2
        get() = end

    override fun drawDebugInfo(graphics: Graphics2D) {
        graphics.stroke = BasicStroke(1f)
        graphics.color = Color(125, 216, 171)
        graphics.draw(Line2D.Double(start.x, start.y, end.x, end.y))
    }

}