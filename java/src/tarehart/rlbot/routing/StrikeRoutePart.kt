package tarehart.rlbot.routing

import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.Duration
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D


class StrikeRoutePart(override val start: Vector2, val intercept: Vector3, strikeProfile: StrikeProfile) : RoutePart {

    override val duration = strikeProfile.strikeDuration
    override val end = intercept.flatten()
    override val waypoint = end

    override fun drawDebugInfo(graphics: Graphics2D) {
        graphics.stroke = BasicStroke(3f)
        graphics.color = Color(214, 206, 243)
        graphics.draw(Line2D.Double(start.x, start.y, end.x, end.y))
    }

}