package tarehart.rlbot.routing

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import java.awt.Graphics2D

class SteerPlan(val immediateSteer: AgentOutput, val route: Route) {

    val waypoint: Vector2
        get() {
            for (part: RoutePart in route.parts) {
                part.waypoint?.let { return it }
            }
            throw NoSuchElementException("No route parts with a waypoint!")
        }

    fun drawDebugInfo(graphics: Graphics2D, car: CarData) {
        route.drawDebugInfo(graphics)
    }
}
