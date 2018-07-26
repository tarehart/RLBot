package tarehart.rlbot.routing.waypoint

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.routing.SteerPlan
import tarehart.rlbot.time.GameTime

abstract class PreKickWaypoint(val position: Vector2, val expectedTime: GameTime, val waitUntil: GameTime? = null) {
    abstract fun planRoute(car: CarData, distancePlot: DistancePlot): SteerPlan
    abstract fun isPlausibleFinalApproach(car: CarData): Boolean
}