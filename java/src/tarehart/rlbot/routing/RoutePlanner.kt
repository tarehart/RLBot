package tarehart.rlbot.routing

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath


object RoutePlanner{

    fun arriveWithSpeed(start: Vector2, end: Vector2, speed:Double, distancePlot: DistancePlot): List<RoutePart>? {

        val distanceDuration = getDecelerationDistanceWhenTargetingSpeed(start, end, speed, distancePlot)

        val distance = start.distance(end)
        val maxAccelMotion = distancePlot.getMotionAfterDistance(distance) ?: return null

        if (distanceDuration.distance == 0.0) {
            return listOf(AccelerationRoutePart(start, end, maxAccelMotion.time))
        }

        if (distanceDuration.distance > distance) {
            // This may overestimate the time if we can't decelerate fast enough
            return listOf(DecelerationRoutePart(start, end, distanceDuration.duration))
        }

        val inflectionPoint = end + (start - end).scaledToMagnitude(distanceDuration.distance)
        val inflectionTime = distancePlot.getTravelTime(distance - distanceDuration.distance) ?: return null

        return listOf(
                AccelerationRoutePart(start, inflectionPoint, inflectionTime),
                DecelerationRoutePart(inflectionPoint, end, distanceDuration.duration))
    }

    fun getDecelerationDistanceWhenTargetingSpeed(start: Vector2, end: Vector2, desiredSpeed:Double, distancePlot: DistancePlot): DistanceDuration {

        val distance = start.distance(end)
        val maxAccelMotion = distancePlot.getMotionAfterDistance(distance) ?: return DistanceDuration(0.0, Duration.ofMillis(0))

        if (maxAccelMotion.speed <= desiredSpeed) {
            return DistanceDuration(0.0, Duration.ofMillis(0))
        }

        val deceleration = ManeuverMath.BRAKING_DECELERATION
        val speedDiff = maxAccelMotion.speed - desiredSpeed
        val secondsDecelerating = speedDiff / deceleration
        val avgSpeed = (maxAccelMotion.speed + desiredSpeed) / 2
        return DistanceDuration(avgSpeed * secondsDecelerating, Duration.ofSeconds(secondsDecelerating))
    }

    fun planRoute(car: CarData, distancePlot: DistancePlot, launchPad: StrikePoint): SteerPlan {
        val toPad = launchPad.position - car.position.flatten()
        val approachError = Vector2.angle(toPad, launchPad.facing)
        val orientationError = Vector2.angle(car.orientation.noseVector.flatten(), launchPad.facing)

        if (orientationError < Math.PI / 12) {
            if (approachError < Math.PI / 12) {
                val duration = distancePlot.getMotionAfterDistance(toPad.magnitude())!!.time
                val route = Route().withPart(AccelerationRoutePart(car.position.flatten(), launchPad.position, duration))
                return SteerPlan(SteerUtil.getThereOnTime(car, SpaceTime(launchPad.position.toVector3(), launchPad.gameTime)), route)
            }
            if (toPad.magnitude() < 2) {
                return SteerPlan(AgentOutput().withThrottle(1.0).withBoost(), Route())
            }
        }

        return CircleTurnUtil.getPlanForCircleTurn(car, distancePlot, launchPad)
    }
}