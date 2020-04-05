package tarehart.rlbot.routing.waypoint

import rlbot.render.Renderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

abstract class PreKickWaypoint(
        val position: Vector2,
        facing: Vector2,
        val expectedTime: GameTime,
        val expectedSpeed: Float? = null,
        val waitUntil: GameTime? = null) {

    val facing = facing.normalized()

    abstract fun planRoute(car: CarData, distancePlot: DistancePlot): AgentOutput
    abstract fun isPlausibleFinalApproach(car: CarData): Boolean

    open fun renderDebugInfo(renderer: Renderer) {
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, position.withZ(1.0)), 2.0, Color.WHITE)
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, position.withZ(1.0)), 1.0, Color.WHITE)
        RenderUtil.drawImpact(renderer, position.withZ(ManeuverMath.BASE_CAR_Z), facing.toVector3(), Color.WHITE)
    }
}
