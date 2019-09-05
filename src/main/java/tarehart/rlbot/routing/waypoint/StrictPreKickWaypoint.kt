package tarehart.rlbot.routing.waypoint

import rlbot.render.Renderer
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.*
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

class StrictPreKickWaypoint(position: Vector2, facing: Vector2, expectedTime: GameTime, expectedSpeed: Double? = null, waitUntil: GameTime? = null) :
        PreKickWaypoint(position, facing, expectedTime, expectedSpeed, waitUntil) {

    override fun isPlausibleFinalApproach(car: CarData): Boolean {
        if (ArenaModel.isCarOnWall(car)) return false
        val tminus = Duration.between(car.time, expectedTime).millis
        if (tminus > 200 || tminus < -50) return false
        val distance = car.position.flatten().distance(position)
        if (distance > 4) return false

        if (distance < 2) {
            // If we're really close, stop caring about whether the car is pointed at the waypoint,
            // and start caring about whether the car is close to the final expected orientation.
            // Don't be TOO strict because the car could be curving in.
            return Math.abs(car.orientation.noseVector.flatten().correctionAngle(facing)) < Math.PI / 8
        }
        val angleError = Math.abs(SteerUtil.getCorrectionAngleRad(car, position))
        val plausibleApproachAngle = angleError < Math.PI / 6 && tminus > 0
        val plausibleBlowPastAngle = angleError > Math.PI * 11.0 / 12.0
        if (!(plausibleApproachAngle || plausibleBlowPastAngle)) return false

        return true
    }

    val positionFacing = PositionFacing(position, facing)

    override fun planRoute(car: CarData, distancePlot: DistancePlot): SteerPlan {
        val flatPosition = car.position.flatten()
        val toPad = this.position - flatPosition
        val orientationError = Vector2.angle(car.orientation.noseVector.flatten(), this.facing)
        val approachError = Vector2.angle(toPad, this.facing)

        val blewPast = ManeuverMath.hasBlownPast(car, this.position, this.facing)

        if (orientationError < Math.PI / 12 && (toPad.magnitude() < 5 || approachError < Math.PI / 20 || blewPast)) {

            val waypoint = if (blewPast) {
                flatPosition + this.facing
            } else {
                this.position
            }

            val orientDuration = AccelerationModel.getOrientDuration(car, waypoint.toVector3())

            val duration = distancePlot.getMotionAfterDistance(toPad.magnitude())?.time ?: distancePlot.getEndPoint().time
            if (this.waitUntil != null) {
                val route = Route()
                        .withPart(OrientRoutePart(flatPosition, orientDuration))
                        .withPart(AccelerationRoutePart(flatPosition, waypoint, duration))
                return SteerPlan(SteerUtil.getThereOnTime(car, SpaceTime(waypoint.toVector3(), this.waitUntil)), route)
            }

            val route = Route()
                    .withPart(OrientRoutePart(flatPosition, orientDuration))
                    .withPart(AccelerationRoutePart(flatPosition, waypoint, duration))
            return SteerPlan(SteerUtil.steerTowardGroundPosition(car, waypoint, detourForBoost = false), route)
        }

        return CircleTurnUtil.getPlanForCircleTurn(car, distancePlot, this)
    }

    override fun renderDebugInfo(renderer: Renderer) {
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, position.toVector3()), 2.0, Color.ORANGE)
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, position.toVector3()), 1.0, Color.ORANGE)
        RenderUtil.drawImpact(renderer, (position + facing).toVector3(), facing.toVector3().scaledToMagnitude(2.0), Color.ORANGE)
    }
}
