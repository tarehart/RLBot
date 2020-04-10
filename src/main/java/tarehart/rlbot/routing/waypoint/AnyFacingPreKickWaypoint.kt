package tarehart.rlbot.routing.waypoint

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath

class AnyFacingPreKickWaypoint(position: Vector2, idealFacing: Vector2, private val allowableFacingError: Float, expectedTime: GameTime, expectedSpeed: Float? = null, waitUntil: GameTime? = null) :
        PreKickWaypoint(position, idealFacing, expectedTime, expectedSpeed, waitUntil) {


    override fun planRoute(car: CarData, distancePlot: DistancePlot): AgentOutput {
        val distance = this.position.distance(car.position.flatten())

        val blewPast = ManeuverMath.hasBlownPast(car, this.position)

        val waypoint = if (blewPast && waitUntil == null) {
            car.position.flatten() + car.orientation.noseVector.flatten()
        } else {
            this.position
        }

        if (this.waitUntil != null) {
            return SteerUtil.getThereOnTime(car, SpaceTime(waypoint.toVector3(), this.waitUntil))
        }

        if (distance > 70 && car.boost < 50) {
            return SteerUtil.steerTowardGroundPosition(car, waypoint)
        }

        return SteerUtil.steerTowardGroundPosition(car, waypoint, detourForBoost = false)

    }
}
