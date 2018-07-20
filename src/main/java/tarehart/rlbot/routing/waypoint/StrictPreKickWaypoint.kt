package tarehart.rlbot.routing.waypoint

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.*
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath

class StrictPreKickWaypoint(position: Vector2, facing: Vector2, expectedTime: GameTime, waitUntil: GameTime? = null) :
        PreKickWaypoint(position, expectedTime, waitUntil) {

    val facing: Vector2 = facing.normalized()
    val positionFacing = PositionFacing(position, facing)

    override fun planRoute(car: CarData, distancePlot: DistancePlot): SteerPlan {
        val toPad = this.position - car.position.flatten()
        val orientationError = Vector2.angle(car.orientation.noseVector.flatten(), this.facing)

        if (orientationError < Math.PI / 12 && toPad.magnitude() < 5) {

//            if (ManeuverMath.hasBlownPast(car, this.position, this.facing)) {
//
//            }

            val waypoint = if (ManeuverMath.hasBlownPast(car, this.position, this.facing)) {
                car.position.flatten() + this.facing
            } else {
                this.position
            }

            if (this.waitUntil != null) {
                val route = Route().withPart(AccelerationRoutePart(car.position.flatten(), waypoint, this.waitUntil - car.time))
                return SteerPlan(SteerUtil.getThereOnTime(car, SpaceTime(waypoint.toVector3(), this.waitUntil)), route)
            }
            val duration = distancePlot.getMotionAfterDistance(toPad.magnitude())!!.time
            val route = Route().withPart(AccelerationRoutePart(car.position.flatten(), waypoint, duration))
            return SteerPlan(SteerUtil.steerTowardGroundPosition(car, waypoint + this.facing.scaled(100.0)), route)
        }

        return CircleTurnUtil.getPlanForCircleTurn(car, distancePlot, this)
    }
}