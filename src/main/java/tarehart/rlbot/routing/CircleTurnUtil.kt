package tarehart.rlbot.routing

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.waypoint.StrictPreKickWaypoint
import tarehart.rlbot.time.Duration

object CircleTurnUtil {

    // Speed 46: radius 46.73
    // Speed 0: radius 7

    // TODO: maybe it ought to be tighter, e.g. A = .018 B = .16
    private val TURN_RADIUS_A = .0153
    private val TURN_RADIUS_B = .16
    private val TURN_RADIUS_C = 7.0

    private fun planWithinCircle(car: CarData, strikePoint: StrictPreKickWaypoint, currentSpeed: Double): SteerPlan {

        val targetPosition = strikePoint.position
        val targetFacing = strikePoint.facing
        val targetNose = targetPosition.plus(targetFacing)
        val targetTail = targetPosition.minus(targetFacing)

        val flatPosition = car.position.flatten()
        val idealCircle = Circle.getCircleFromPoints(targetTail, targetNose, flatPosition)

        val clockwise = Circle.isClockwise(idealCircle, targetPosition, targetFacing)

        val idealSpeedOption = getSpeedForRadius(idealCircle.radius)

        val idealSpeed = idealSpeedOption ?: 10.0

        val speedRatio = currentSpeed / idealSpeed // Ideally should be 1

        val lookaheadRadians = Math.PI / 20
        val centerToSteerTarget = VectorUtil.rotateVector(flatPosition.minus(idealCircle.center), lookaheadRadians * if (clockwise) -1 else 1)
        val steerTarget = idealCircle.center.plus(centerToSteerTarget)

        val output = SteerUtil.steerTowardGroundPosition(car, steerTarget).withBoost(false).withSlide(false).withThrottle(1.0)

        if (speedRatio < 1) {
            output.withBoost(currentSpeed >= AccelerationModel.MEDIUM_SPEED && speedRatio < .8 || speedRatio < .7)
        } else {
            val framesBetweenSlidePulses: Int
            if (speedRatio > 2) {
                framesBetweenSlidePulses = 3
                output.withThrottle(0.0)
            } else if (speedRatio > 1.5) {
                framesBetweenSlidePulses = 6
            } else if (speedRatio > 1.2) {
                framesBetweenSlidePulses = 9
            } else {
                framesBetweenSlidePulses = 12
            }
            output.withSlide(car.frameCount % (framesBetweenSlidePulses + 1) == 0L)
        }

        val turnDuration = getTurnDuration(idealCircle, flatPosition, targetPosition, clockwise, idealSpeed)

        // TODO: sometimes this turn duration gets really big at the end of a circle turn that seems to be going fine.
        val route = Route()
                .withPart(CircleRoutePart(
                        start = flatPosition,
                        end = targetPosition,
                        duration = turnDuration,
                        circle = idealCircle,
                        clockwise = clockwise))

        return SteerPlan(output, route)
    }

    private fun getTurnDuration(circle: Circle, start: Vector2, end: Vector2, clockwise: Boolean, speed: Double): Duration {
        return Duration.ofSeconds(Math.abs(getSweepRadians(circle, start, end, clockwise)) * circle.radius / speed)
    }

    fun getSweepRadians(circle: Circle, start: Vector2, end: Vector2, clockwise: Boolean): Double {
        val centerToStart = start - circle.center
        val centerToEnd = end - circle.center

        return centerToStart.correctionAngle(centerToEnd, clockwise)
    }

    private fun getTurnRadius(speed: Double): Double {
        return TURN_RADIUS_A * speed * speed + TURN_RADIUS_B * speed + TURN_RADIUS_C
    }

    private fun getSpeedForRadius(radius: Double): Double? {

        if (radius == TURN_RADIUS_C) {
            return 0.0
        }

        if (radius < TURN_RADIUS_C) {
            return null
        }

        val a = TURN_RADIUS_A
        val b = TURN_RADIUS_B
        val c = TURN_RADIUS_C - radius

        val p = -b / (2 * a)
        val q = Math.sqrt(b * b - 4.0 * a * c) / (2 * a)
        return p + q
    }

    fun getPlanForCircleTurn(
            car:CarData, distancePlot: DistancePlot, strikePoint: StrictPreKickWaypoint): SteerPlan {

        val targetPosition = strikePoint.position
        val distance = car.position.flatten().distance(targetPosition)
        val maxSpeed = distancePlot.getMotionAfterDistance(distance)?.speed ?: AccelerationModel.SUPERSONIC_SPEED
        val idealSpeed = getIdealCircleSpeed(PositionFacing(car.position.flatten(), car.orientation.noseVector.flatten()), strikePoint.positionFacing)
        val currentSpeed = car.velocity.flatten().magnitude()

        return circleWaypoint(car, strikePoint, currentSpeed, Math.min(maxSpeed, idealSpeed), distancePlot)
    }

    private fun getIdealCircleSpeed(currentFacing: PositionFacing, targetFacing: PositionFacing): Double {

        val estimatedEntryAngle = estimateApproachVector(currentFacing, targetFacing.position)

        val orientationCorrection = estimatedEntryAngle.correctionAngle(targetFacing.facing)
        val angleAllowingFullSpeed = Math.PI / 6
        val speedPenaltyPerRadian = 20.0
        val rawPenalty = speedPenaltyPerRadian * (Math.abs(orientationCorrection) - angleAllowingFullSpeed)
        val correctionPenalty = Math.max(0.0, rawPenalty)
        return Math.max(15.0, AccelerationModel.SUPERSONIC_SPEED - correctionPenalty)
    }

    fun estimateApproachVector(currentFacing: PositionFacing, target: Vector2): Vector2 {
        // When we are close to the target, the current orientation matters, and is probably taking the circle
        // tangent point into account if we have been approaching the circle for a few frames, so currentFacing
        // is most accurate.

        // When we are far from the target, we should bias for the approach angle because it guards us against the
        // situation where we just found out about the circle and the car is pointed the completely wrong way. Also
        // the approach angle is a pretty good approximation when we are far away.

        // This summation leads to a weighted average of the two vectors depending on distance.

        // TODO: make sure this still makes sense in a route-aware planning context
        val toTarget = target - currentFacing.position
        val estimatedEntryAngle = currentFacing.facing + toTarget.scaled(0.05)
        return estimatedEntryAngle.normalized()
    }

    private fun circleWaypoint(
            car: CarData,
            strikePoint: StrictPreKickWaypoint,
            currentSpeed: Double,
            expectedSpeed: Double,
            distancePlot: DistancePlot): SteerPlan {

        val targetPosition = strikePoint.position
        val targetFacing = strikePoint.facing

        val flatPosition = car.position.flatten()
        val toTarget = targetPosition.minus(flatPosition)

        val correctionAngle = toTarget.correctionAngle(targetFacing)
        if (correctionAngle == 0.0) {
            return planDirectRoute(flatPosition, car, strikePoint, distancePlot, toTarget)
        }

        val clockwise = toTarget.correctionAngle(targetFacing) < 0

        val turnRadius = getTurnRadius(expectedSpeed)
        // Make sure the radius vector points from the target position to the center of the turn circle.
        val radiusVector = VectorUtil.rotateVector(targetFacing, Math.PI / 2 * if (clockwise) -1 else 1).scaled(turnRadius)

        val center = targetPosition.plus(radiusVector)
        val circle = Circle(center, turnRadius)

        val tangentPoints = circle.calculateTangentPoints(flatPosition) ?:
            return planWithinCircle(car, strikePoint, currentSpeed)

        val toCenter = center.minus(flatPosition)

        val tangentPoint = if ((tangentPoints.first - flatPosition).correctionAngle(toCenter) < 0 == clockwise)
            tangentPoints.first
        else
            tangentPoints.second

        val distanceToTangent = tangentPoint.distance(flatPosition)
        val turnDuration = getTurnDuration(circle, tangentPoint, targetPosition, clockwise, expectedSpeed)

        val immediateSteer: AgentOutput
        if (strikePoint.waitUntil != null) {
            val momentToStartTurning = strikePoint.waitUntil - turnDuration
            immediateSteer = SteerUtil.getThereOnTime(car, SpaceTime(tangentPoint.toVector3(), momentToStartTurning))
        } else {
            immediateSteer = SteerUtil.steerTowardGroundPosition(car, tangentPoint)
        }

        if (currentSpeed > expectedSpeed && distanceToTangent < 20) {
            immediateSteer.withThrottle(-1.0)  // TODO: This is probably out of place, we have more subtle ways of doing this now
        }


        val route = Route()
        route.withPart(OrientRoutePart(flatPosition, Duration.ofSeconds(AccelerationModel.getSteerPenaltySeconds(car, tangentPoint.toVector3()))))

        val accelerationTime = distancePlot.getTravelTime(distanceToTangent) ?: distancePlot.getEndPoint().time
        val arrivalParts = RoutePlanner.arriveWithSpeed(flatPosition, tangentPoint, expectedSpeed, distancePlot) ?:
            listOf(AccelerationRoutePart(flatPosition, tangentPoint, accelerationTime))

        arrivalParts.forEach { route.withPart(it) }
        route.withPart(CircleRoutePart(
                start = tangentPoint,
                end = targetPosition,
                duration = turnDuration,
                circle = circle,
                clockwise = clockwise))

        return SteerPlan(immediateSteer, route)
    }

    private fun planDirectRoute(flatPosition: Vector2, car: CarData, strikePoint: StrictPreKickWaypoint, distancePlot: DistancePlot, toTarget: Vector2): SteerPlan {
        val route = Route()
        route.withPart(OrientRoutePart(flatPosition, Duration.ofSeconds(AccelerationModel.getSteerPenaltySeconds(car, strikePoint.position.toVector3()))))

        val accelerationTime = distancePlot.getTravelTime(toTarget.magnitude())

        route.withPart(AccelerationRoutePart(flatPosition, strikePoint.position, accelerationTime!!))

        val immediateSteer: AgentOutput = if (strikePoint.waitUntil != null) {
            SteerUtil.getThereOnTime(car, SpaceTime(strikePoint.position.toVector3(), strikePoint.waitUntil))
        } else {
            SteerUtil.steerTowardGroundPosition(car, strikePoint.position)
        }

        return SteerPlan(immediateSteer, route)
    }
}
