package tarehart.rlbot.routing

import tarehart.rlbot.AgentInput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration

object CircleTurnUtil {
    private val TURN_RADIUS_A = .0153
    private val TURN_RADIUS_B = .16
    private val TURN_RADIUS_C = 7.0

    private fun planWithinCircle(car: CarData, strikePoint: StrikePoint, currentSpeed: Double): SteerPlan {

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

        val output = SteerUtil.steerTowardGroundPosition(car, steerTarget).withBoost(false).withSlide(false).withDeceleration(0.0).withAcceleration(1.0)

        if (speedRatio < 1) {
            output.withBoost(currentSpeed >= AccelerationModel.MEDIUM_SPEED && speedRatio < .8 || speedRatio < .7)
        } else {
            val framesBetweenSlidePulses: Int
            if (speedRatio > 2) {
                framesBetweenSlidePulses = 3
                output.withAcceleration(0.0)
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
        return SteerPlan(output, Route(car.time)
                .withPart(CircleRoutePart(
                        start = flatPosition,
                        end = targetPosition,
                        duration = turnDuration,
                        circle = idealCircle,
                        clockwise = clockwise)))
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
            input: AgentInput, distancePlot: DistancePlot, strikePoint: StrikePoint): SteerPlan {

        val car = input.myCarData
        val targetPosition = strikePoint.position
        val distance = car.position.flatten().distance(targetPosition)
        val maxSpeed = distancePlot.getMotionAfterDistance(distance)?.speed ?: AccelerationModel.SUPERSONIC_SPEED
        val idealSpeed = getIdealCircleSpeed(PositionFacing(car.position.flatten(), car.orientation.noseVector.flatten()), strikePoint.positionFacing)
        val currentSpeed = car.velocity.magnitude()

        return circleWaypoint(input, strikePoint, currentSpeed, Math.min(maxSpeed, idealSpeed), distancePlot)
    }

    private fun getIdealCircleSpeed(currentFacing: PositionFacing, targetFacing: PositionFacing): Double {
        val approachAngle = targetFacing.position - currentFacing.position

        // When we are close to the target, the current orientation matters, and is probably taking the circle
        // tangent point into account if we have been approaching the circle for a few frames, so currentFacing
        // is most accurate.

        // When we are far from the target, we should bias for the approach angle because it guards us against the
        // situation where we just found out about the circle and the car is pointed the completely wrong way. Also
        // the approach angle is a pretty good approximation when we are far away.

        // This summation leads to a weighted average of the two vectors depending on distance.
        val estimatedEntryAngle = currentFacing.facing + approachAngle.scaled(0.05)

        val orientationCorrection = estimatedEntryAngle.correctionAngle(targetFacing.facing)
        val angleAllowingFullSpeed = Math.PI / 6
        val speedPenaltyPerRadian = 20.0
        val rawPenalty = speedPenaltyPerRadian * (Math.abs(orientationCorrection) - angleAllowingFullSpeed)
        val correctionPenalty = Math.max(0.0, rawPenalty)
        return Math.max(15.0, AccelerationModel.SUPERSONIC_SPEED - correctionPenalty)
    }

    private fun circleWaypoint(
            input: AgentInput,
            strikePoint: StrikePoint,
            currentSpeed: Double,
            expectedSpeed: Double,
            distancePlot: DistancePlot): SteerPlan {

        val targetPosition = strikePoint.position
        val targetFacing = strikePoint.facing
        val car = input.myCarData

        val flatPosition = car.position.flatten()
        val toTarget = targetPosition.minus(flatPosition)

        val clockwise = toTarget.correctionAngle(targetFacing) < 0

        val turnRadius = getTurnRadius(expectedSpeed)
        // Make sure the radius vector points from the target position to the center of the turn circle.
        val radiusVector = VectorUtil.rotateVector(targetFacing, Math.PI / 2 * if (clockwise) -1 else 1).scaled(turnRadius)

        val center = targetPosition.plus(radiusVector)
        val circle = Circle(center, turnRadius)

        val tangentPoints = circle.calculateTangentPoints(flatPosition) ?:
            return if (currentSpeed < expectedSpeed) {
                circleWaypoint(input, strikePoint, currentSpeed, currentSpeed, distancePlot)
            } else planWithinCircle(car, strikePoint, currentSpeed)

        val toCenter = center.minus(flatPosition)

        val tangentPoint = if ((tangentPoints.first - flatPosition).correctionAngle(toCenter) < 0 == clockwise)
            tangentPoints.first
        else
            tangentPoints.second

        val toTangent = tangentPoint.minus(flatPosition)
        val turnDuration = getTurnDuration(circle, flatPosition, targetPosition, clockwise, expectedSpeed)

        val momentToStartTurning = strikePoint.gameTime.minus(turnDuration)
        val immediateSteer = SteerUtil.getThereOnTime(car, SpaceTime(tangentPoint.toVector3(), momentToStartTurning), input.boostData)
        if (currentSpeed > expectedSpeed && toTangent.magnitude() < 20) {
            immediateSteer.withAcceleration(0.0).withDeceleration(1.0)
        }



        val route = Route(strikePoint.gameTime, workBackwards = true)
        route.withPart(OrientRoutePart(flatPosition, Duration.ofSeconds(AccelerationModel.getSteerPenaltySeconds(car, tangentPoint.toVector3()))))

        val arrivalParts = RoutePlanner.arriveWithSpeed(flatPosition, tangentPoint, expectedSpeed, distancePlot) ?:
            listOf(AccelerationRoutePart(flatPosition, tangentPoint, Duration.between(car.time, strikePoint.gameTime - turnDuration)))

        arrivalParts.forEach { route.withPart(it) }
        route.withPart(CircleRoutePart(
                start = tangentPoint,
                end = targetPosition,
                duration = turnDuration,
                circle = circle,
                clockwise = clockwise))

        return SteerPlan(immediateSteer, route)
    }

    fun getPlanForCircleTurn(input: AgentInput, distancePlot: DistancePlot, flatten: Vector2, facing: Vector2): SteerPlan {
        return getPlanForCircleTurn(input, distancePlot, StrikePoint(flatten, facing, input.time))
    }
}
