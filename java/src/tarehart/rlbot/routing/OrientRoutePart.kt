package tarehart.rlbot.routing

import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.Duration
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D

/**
 * This is somewhat degenerate. It models the time required to change the facing of the car and make up for
 * the resulting displacement and loss of forward speed.
 */
class OrientRoutePart(
        override val start: Vector2,
        override val duration: Duration) : RoutePart {

    override val end: Vector2
        get() = start

    override val waypoint: Vector2? = null

    override fun drawDebugInfo(graphics: Graphics2D) {

        graphics.color = Color(125, 50, 50)
        graphics.draw(Circle(start, 1.0).toShape())
    }

}