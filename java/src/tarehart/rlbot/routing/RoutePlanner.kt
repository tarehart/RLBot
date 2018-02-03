package tarehart.rlbot.routing

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath


object RoutePlanner{

    fun arriveWithSpeed(start: Vector2, end: Vector2, speed:Double, distancePlot: DistancePlot): List<RoutePart>? {

        val distance = start.distance(end)
        val maxAccelMotion = distancePlot.getMotionAfterDistance(distance) ?: return null

        if (maxAccelMotion.speed <= speed) {
            return listOf(AccelerationRoutePart(start, end, maxAccelMotion.time))
        }

        val deceleration = ManeuverMath.BRAKING_DECELERATION
        val speedDiff = maxAccelMotion.speed - speed
        val secondsDecelerating = speedDiff / deceleration
        val decelerationDuration = Duration.ofSeconds(secondsDecelerating)
        val avgSpeed = speedDiff / 2
        val decelerationDistance = avgSpeed * secondsDecelerating

        if (decelerationDistance > distance) {
            // This may overestimate the time if we can't decelerate fast enough
            return listOf(DecelerationRoutePart(start, end, decelerationDuration))
        }

        val inflectionPoint = end + (start - end).scaledToMagnitude(decelerationDistance)
        val inflectionTime = distancePlot.getTravelTime(distance - decelerationDistance) ?: return null

        return listOf(
                AccelerationRoutePart(start, inflectionPoint, inflectionTime),
                DecelerationRoutePart(inflectionPoint, end, decelerationDuration))
    }


    fun planRoute(car: CarData, target: StrikePoint): Route {

        return Route(car.time)
    }

    fun planRoute(car: CarData, target: PositionFacing, boostBudget: Double): Route {

        // TODO: do something special if we're super close



        // We'll decide what to do based on approach error
        val position = car.position.flatten()
        val directLine = target.position - position
        val distance = directLine.magnitude()
        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(6.0), boostBudget, distance)

        val arrivalCorrection = directLine.correctionAngle(target.facing)
        val arrivalCorrectionMagnitude = Math.abs(arrivalCorrection)

        if (arrivalCorrectionMagnitude < Math.PI / 12) {
            // We can drive there in a straight line!

            val orientSeconds = AccelerationModel.getSteerPenaltySeconds(car, target.position.toVector3())

            val duration = distancePlot.getMotionAfterDistance(distance)?.time?.plusSeconds(orientSeconds) ?: Duration.ofSeconds(6.0)

            distancePlot.getMotionAfterDistance(distance)
            return Route(car.time).withPart(AccelerationRoutePart(position, target.position, duration))
        }

        if (arrivalCorrectionMagnitude < Math.PI / 4) {
            // We can reasonably do a circle turn

        }

        return Route(car.time)

    }

}