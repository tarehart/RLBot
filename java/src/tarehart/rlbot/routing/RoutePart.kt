package tarehart.rlbot.routing

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.Duration
import java.awt.Graphics2D

interface RoutePart {

    val start: Vector2
    val end: Vector2
    val duration: Duration
    val waypoint: Vector2

    fun drawDebugInfo(graphics: Graphics2D)
}