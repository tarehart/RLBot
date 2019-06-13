package tarehart.rlbot.routing

import rlbot.render.Renderer
import tarehart.rlbot.math.Atan
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import java.awt.Color
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
            val startRadians = Atan.atan2(centerToStart.y, centerToStart.x)

            val startDegrees = -startRadians * 180 / Math.PI
            val sweepDegrees = -sweepRadians * 180 / Math.PI

            val arc = Arc2D.Double(circle.toShape().bounds2D, startDegrees, sweepDegrees, Arc2D.OPEN)
            graphics.draw(arc)
        }
    }

    override fun renderDebugInfo(renderer: Renderer) {
        if (start.distance(end) > .01) {

            val centerToStart = start - circle.center

            var cursor = centerToStart.scaledToMagnitude(circle.radius)
            var radians = 0.0

            while (radians < Math.abs(sweepRadians)) {
                radians += 0.1
                val nextCursor = VectorUtil.rotateVector(cursor, 0.1 * Math.signum(sweepRadians))

                RenderUtil.drawSquare(
                        renderer,
                        Plane((nextCursor - cursor).toVector3(), (circle.center + cursor).withZ(2.0)),
                        2.0,
                        Color.MAGENTA)
                renderer.drawLine3d(Color.MAGENTA, (circle.center + cursor).toVector3().toRlbot(), (circle.center + nextCursor).toVector3().toRlbot())
                cursor = nextCursor
            }
        }
    }

}
