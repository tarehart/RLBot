package tarehart.rlbot.routing.waypoint

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.*
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class FacingAndSpeedPreKickWaypoint(position: Vector2, expectedTime: GameTime, facing: Vector2, val speed: Double) :
        PreKickWaypoint(position, facing, expectedTime, speed) {

    override fun isPlausibleFinalApproach(car: CarData): Boolean {

        if (ArenaModel.isCarOnWall(car)) return false
        return Vector2.angle(car.orientation.noseVector.flatten(), facing) < Math.PI / 30 &&
                Math.abs(speed - car.velocity.magnitude()) < 1
    }

    override fun planRoute(car: CarData, distancePlot: DistancePlot): SteerPlan {

        val carSpeed = car.velocity.magnitude()

        val immediateSteer = SteerUtil.steerTowardGroundPosition(car, car.position.flatten() + facing, false)
                .withThrottle(speed - carSpeed)
                .withBoost(speed > AccelerationModel.MEDIUM_SPEED && speed > carSpeed)

        val route = Route()

        RoutePlanner.getMotionAfterSpeedChange(carSpeed, speed, distancePlot)?.let {
            val positionAfterSpeedChange = car.position.flatten() + facing.scaledToMagnitude(it.distance)
            if (speed > carSpeed) {
                route.withPart(AccelerationRoutePart(
                        car.position.flatten(),
                        positionAfterSpeedChange,
                        it.time))
            } else {
                route.withPart(DecelerationRoutePart(
                        car.position.flatten(),
                        positionAfterSpeedChange,
                        it.time))
            }

            val orientDuration = getOrientDuration(car.orientation.noseVector.flatten(), facing)
            if (orientDuration > it.time)
            route.withPart(OrientRoutePart(
                    positionAfterSpeedChange,
                    orientDuration - it.time))
        }

        return SteerPlan(immediateSteer, route)

    }

    companion object {
        fun getOrientDuration(currentFacing: Vector2, idealFacing: Vector2): Duration {
            val facingError = Vector2.angle(currentFacing, idealFacing)
            return Duration.ofSeconds(facingError * 0.2)
        }
    }
}
