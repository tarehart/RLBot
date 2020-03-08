package tarehart.rlbot.routing

import rlbot.render.Renderer
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.rendering.RenderUtil
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
        graphics.draw(Line2D.Float(start.x, start.y, end.x, end.y))
    }

    override fun renderDebugInfo(renderer: Renderer) {

        val toStart = start.toVector3() - end.toVector3()
        if (toStart.isZero) {
            return
        }
        val toStartNormal = toStart.normaliseCopy()
        val raisedEnd = end.withZ(2.0)
        val distance = toStart.magnitude()
        var distanceCovered = 4.0F
        while (distanceCovered < distance) {
            RenderUtil.drawSquare(renderer, Plane(toStart.normaliseCopy(), raisedEnd + toStartNormal.scaled(distanceCovered)), 2.0, Color.GREEN)
            distanceCovered *= 1.5F
        }
    }

}
