package tarehart.rlbot.routing.waypoint

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.AccelerationRoutePart
import tarehart.rlbot.routing.Route
import tarehart.rlbot.routing.SteerPlan
import tarehart.rlbot.time.GameTime

class AnyFacingPreKickWaypoint(position: Vector2, expectedTime: GameTime, waitUntil: GameTime? = null) :
        PreKickWaypoint(position, expectedTime, waitUntil) {

    override fun planRoute(car: CarData, distancePlot: DistancePlot): SteerPlan {
        val distance = this.position.distance(car.position.flatten())
        val waypoint = this.position

        if (this.waitUntil != null) {
            val route = Route().withPart(AccelerationRoutePart(car.position.flatten(), waypoint, this.waitUntil - car.time))
            return SteerPlan(SteerUtil.getThereOnTime(car, SpaceTime(waypoint.toVector3(), this.waitUntil)), route)
        }
        val duration = distancePlot.getMotionAfterDistance(distance)!!.time
        val route = Route().withPart(AccelerationRoutePart(car.position.flatten(), waypoint, duration))
        return SteerPlan(SteerUtil.steerTowardGroundPosition(car, waypoint), route)

    }
}