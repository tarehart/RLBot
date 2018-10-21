package tarehart.rlbot.routing

import rlbot.render.Renderer
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.Duration
import java.awt.Graphics2D
import java.awt.geom.Arc2D


class CircleRoutePart(
        override val start: Vector2,
        override val end: Vector2,
        override val duration: Duration,
        val circle: Circle,
        val clockwise: Boolean) : RoutePart {
    override val waypoint: Vector2
        get() = start

    val sweepRadians = CircleTurnUtil.getSweepRadians(circle, start, end, clockwise)

    override fun drawDebugInfo(graphics: Graphics2D) {
        if (start.distance(end) > .01) {

            val centerToStart = start - circle.center
            val startRadians = Math.atan2(centerToStart.y, centerToStart.x)

            val startDegrees = -startRadians * 180 / Math.PI
            val sweepDegrees = -sweepRadians * 180 / Math.PI

            val arc = Arc2D.Double(circle.toShape().bounds2D, startDegrees, sweepDegrees, Arc2D.OPEN)
            graphics.draw(arc)
        }
    }

    override fun renderDebugInfo(renderer: Renderer) {

    }

}
