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
import tarehart.rlbot.tuning.ManeuverMath
import kotlin.math.abs
import kotlin.math.max

object CircleTurnUtil {

    // Speed 46: radius 46.73
    // Speed 0: radius 7

    // TODO: maybe it ought to be tighter, e.g. A = .018 B = .16
    private val TURN_RADIUS_A = .0153F
    private val TURN_RADIUS_B = .16F
    private val TURN_RADIUS_C = 7.0F

    private val ENFORCED_CIRCLE_SPEED = 40.0F

    private fun planWithinCircle(car: CarData, strikePoint: StrictPreKickWaypoint, currentSpeed: Float, desiredSpeed: Float, circle: Circle): SteerPlan {

        val targetPosition = strikePoint.position
        val targetFacing = strikePoint.facing
        val targetNose = targetPosition.plus(targetFacing)
        val targetTail = targetPosition.minus(targetFacing)

        val flatPosition = car.position.flatten()
//        val idealCircle = Circle.getCircleFromPoints(targetTail, targetNose, flatPosition)

        val clockwise = Circle.isClockwise(circle, targetPosition, targetFacing)


        val idealSpeed = ENFORCED_CIRCLE_SPEED

        val lookaheadRadians = Math.PI / 8
        val centerToSteerTarget = VectorUtil.rotateVector(flatPosition.minus(circle.center).scaledToMagnitude(circle.radius), lookaheadRadians * if (clockwise) -1 else 1)
        val steerTarget = circle.center.plus(centerToSteerTarget)
        val predictedAvgSpeed = (idealSpeed * 5 + currentSpeed) / 6
        val timeAtTarget = car.time + getTurnDuration(circle, flatPosition, steerTarget, clockwise, predictedAvgSpeed)

        val output = SteerUtil.getThereOnTime(car, SpaceTime(steerTarget.toVector3(), timeAtTarget))

        val turnDuration = getTurnDuration(circle, flatPosition, targetPosition, clockwise, predictedAvgSpeed)

        // TODO: sometimes this turn duration gets really big at the end of a circle turn that seems to be going fine.
        val route = Route()
                .withPart(CircleRoutePart(
                        start = flatPosition,
                        end = targetPosition,
                        duration = turnDuration,
                        circle = circle,
                        clockwise = clockwise))

        return SteerPlan(output, route)
    }

    private fun getTurnDuration(circle: Circle, start: Vector2, end: Vector2, clockwise: Boolean, speed: Float): Duration {
        val sweepRadians = getSweepRadians(circle, start, end, clockwise)
        return Duration.ofSeconds(abs(sweepRadians) * circle.radius / speed)
    }

    fun getSweepRadians(circle: Circle, start: Vector2, end: Vector2, clockwise: Boolean): Float {
        val centerToStart = start - circle.center
        val centerToEnd = end - circle.center

        return centerToStart.correctionAngle(centerToEnd, clockwise)
    }

    private fun getTurnRadius(speed: Float): Float {
        return TURN_RADIUS_A * speed * speed + TURN_RADIUS_B * speed + TURN_RADIUS_C
    }

    private fun getSpeedForRadius(radius: Float): Double? {

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

    private fun getIdealCircleSpeed(currentFacing: PositionFacing, targetFacing: PositionFacing): Float {

        val estimatedEntryAngle = ManeuverMath.estimateApproachVector(currentFacing, targetFacing.position)

        val orientationCorrection = estimatedEntryAngle.correctionAngle(targetFacing.facing)
        val angleAllowingFullSpeed = Math.PI.toFloat() / 6
        val speedPenaltyPerRadian = 20F
        val rawPenalty = speedPenaltyPerRadian * (abs(orientationCorrection) - angleAllowingFullSpeed)
        val correctionPenalty = max(0F, rawPenalty)
        return max(15F, AccelerationModel.SUPERSONIC_SPEED - correctionPenalty)
    }

    private fun circleWaypoint(
            car: CarData,
            strikePoint: StrictPreKickWaypoint,
            currentSpeed: Float,
            expectedSpeed: Float,
            distancePlot: DistancePlot): SteerPlan {

        val targetPosition = strikePoint.position
        val targetFacing = strikePoint.facing

        val flatPosition = car.position.flatten()
        val toTarget = targetPosition.minus(flatPosition)

        val correctionAngle = toTarget.correctionAngle(targetFacing)
        if (correctionAngle == 0F) {
            return planDirectRoute(flatPosition, car, strikePoint, distancePlot, toTarget)
        }

        val clockwise = toTarget.correctionAngle(targetFacing) < 0

        val turnRadius = getTurnRadius(ENFORCED_CIRCLE_SPEED)
        // Make sure the radius vector points from the target position to the center of the turn circle.
        val radiusVector = VectorUtil.rotateVector(targetFacing, Math.PI / 2 * if (clockwise) -1 else 1).scaled(turnRadius)

        val center = targetPosition.plus(radiusVector)
        val circle = Circle(center, turnRadius)

        val tangentPoints = circle.calculateTangentPointsToExternalPoint(flatPosition) ?:
            return planWithinCircle(car, strikePoint, currentSpeed, expectedSpeed, circle)

        val toCenter = center.minus(flatPosition)

        val tangentPoint = if ((tangentPoints.first - flatPosition).correctionAngle(toCenter) < 0 == clockwise)
            tangentPoints.first
        else
            tangentPoints.second

        val distanceToTangent = tangentPoint.distance(flatPosition)

        if (distanceToTangent < 5) {
            return planWithinCircle(car, strikePoint, currentSpeed, expectedSpeed, circle)
        }

        val turnDuration = getTurnDuration(circle, tangentPoint, targetPosition, clockwise, expectedSpeed)

        val immediateSteer: AgentOutput
        if (strikePoint.waitUntil != null) {
            val momentToStartTurning = strikePoint.waitUntil - turnDuration
            immediateSteer = SteerUtil.getThereOnTime(car, SpaceTime(tangentPoint.toVector3(), momentToStartTurning))
        } else {
            immediateSteer = SteerUtil.steerTowardGroundPosition(car, tangentPoint, detourForBoost = false)
        }

        if (currentSpeed > ENFORCED_CIRCLE_SPEED && distanceToTangent < 20) {
            immediateSteer.withThrottle(-1.0)  // TODO: This is probably out of place, we have more subtle ways of doing this now
        }


        val route = Route()
        route.withPart(OrientRoutePart(flatPosition, Duration.ofSeconds(SteerUtil.getSteerPenaltySeconds(car, tangentPoint.toVector3()))))

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
        route.withPart(OrientRoutePart(flatPosition, Duration.ofSeconds(SteerUtil.getSteerPenaltySeconds(car, strikePoint.position.toVector3()))))

        val accelerationTime = distancePlot.getTravelTime(toTarget.magnitude())

        route.withPart(AccelerationRoutePart(flatPosition, strikePoint.position, accelerationTime!!))

        val immediateSteer: AgentOutput = if (strikePoint.waitUntil != null) {
            SteerUtil.getThereOnTime(car, SpaceTime(strikePoint.position.toVector3(), strikePoint.waitUntil))
        } else {
            SteerUtil.steerTowardGroundPosition(car, strikePoint.position, detourForBoost = false)
        }

        return SteerPlan(immediateSteer, route)
    }
}
