package tarehart.rlbot.routing

import rlbot.render.Renderer
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import java.awt.Color
import java.awt.Graphics2D

interface RoutePart {

    val start: Vector2
    val end: Vector2
    val duration: Duration
    val waypoint: Vector2?

    fun drawDebugInfo(graphics: Graphics2D)

    fun renderDebugInfo(renderer: Renderer)
}