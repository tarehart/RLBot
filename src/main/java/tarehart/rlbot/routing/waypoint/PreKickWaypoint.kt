package tarehart.rlbot.routing.waypoint

import rlbot.render.Renderer
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.SteerPlan
import tarehart.rlbot.time.GameTime
import java.awt.Color

abstract class PreKickWaypoint(
        val position: Vector2,
        facing: Vector2,
        val expectedTime: GameTime,
        val expectedSpeed: Double? = null,
        val waitUntil: GameTime? = null) {

    val facing = facing.normalized()

    abstract fun planRoute(car: CarData, distancePlot: DistancePlot): SteerPlan
    abstract fun isPlausibleFinalApproach(car: CarData): Boolean

    open fun renderDebugInfo(renderer: Renderer) {
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, position.toVector3()), 2.0, Color.WHITE)
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, position.toVector3()), 1.0, Color.WHITE)
    }
}