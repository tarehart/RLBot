package tarehart.rlbot.routing

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.Duration
import java.awt.BasicStroke
import java.awt.BasicStroke.CAP_SQUARE
import java.awt.BasicStroke.JOIN_MITER
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D


class DecelerationRoutePart(
        override val start: Vector2,
        override val end: Vector2,
        override val duration: Duration) : RoutePart {

    override val waypoint: Vector2
        get() = end

    override fun drawDebugInfo(graphics: Graphics2D) {
        graphics.stroke = BasicStroke(1f, CAP_SQUARE, JOIN_MITER, 10.0f, floatArrayOf(2f), 0.0f)
        graphics.color = Color(233, 214, 75)
        graphics.draw(Line2D.Double(start.x, start.y, end.x, end.y))
    }

}