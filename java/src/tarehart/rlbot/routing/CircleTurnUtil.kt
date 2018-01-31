package tarehart.rlbot.routing

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
        return Duration.ofSeconds(getSweepRadians(circle, start, end, clockwise) * circle.radius / speed)
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

    private fun getFacingCorrectionSeconds(approach: Vector2, targetFacing: Vector2, expectedSpeed: Double): Double {

        val correction = approach.correctionAngle(targetFacing)
        return getTurnRadius(expectedSpeed) * Math.abs(correction) / expectedSpeed
    }

    fun getPlanForCircleTurn(
            car: CarData, distancePlot: DistancePlot, strikePoint: StrikePoint): SteerPlan {

        val targetPosition = strikePoint.position
        val targetFacing = strikePoint.facing
        val distance = car.position.flatten().distance(targetPosition)
        val maxSpeed = distancePlot.getMotionAfterDistance(distance)
                .map { dts -> dts.speed }
                .orElse(AccelerationModel.SUPERSONIC_SPEED)
        val idealSpeed = getIdealCircleSpeed(car, targetFacing)
        val currentSpeed = car.velocity.magnitude()

        return circleWaypoint(car, strikePoint, currentSpeed, Math.min(maxSpeed, idealSpeed))
    }

    private fun getIdealCircleSpeed(car: CarData, targetFacing: Vector2): Double {
        val orientationCorrection = car.orientation.noseVector.flatten().correctionAngle(targetFacing)
        val angleAllowingFullSpeed = Math.PI / 6
        val speedPenaltyPerRadian = 20.0
        val rawPenalty = speedPenaltyPerRadian * (Math.abs(orientationCorrection) - angleAllowingFullSpeed)
        val correctionPenalty = Math.max(0.0, rawPenalty)
        return Math.max(15.0, AccelerationModel.SUPERSONIC_SPEED - correctionPenalty)
    }

    private fun circleWaypoint(car: CarData, strikePoint: StrikePoint, currentSpeed: Double, expectedSpeed: Double): SteerPlan {

        val targetPosition = strikePoint.position
        val targetFacing = strikePoint.facing

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
                circleWaypoint(car, strikePoint, currentSpeed, currentSpeed)
            } else planWithinCircle(car, strikePoint, currentSpeed)

        val toCenter = center.minus(flatPosition)

        val tangentPoint = if ((tangentPoints.first - flatPosition).correctionAngle(toCenter) < 0 == clockwise)
            tangentPoints.first
        else
            tangentPoints.second

        val toTangent = tangentPoint.minus(flatPosition)
        val facingCorrectionSeconds = getFacingCorrectionSeconds(toTangent, targetFacing, expectedSpeed)

        val momentToStartTurning = strikePoint.gameTime.minusSeconds(facingCorrectionSeconds)
        val immediateSteer = SteerUtil.getThereOnTime(car, SpaceTime(tangentPoint.toVector3(), momentToStartTurning))
        if (currentSpeed > expectedSpeed && toTangent.magnitude() < 20) {
            immediateSteer.withAcceleration(0.0).withDeceleration(1.0)
        }

        val turnDuration = getTurnDuration(circle, flatPosition, targetPosition, clockwise, expectedSpeed)

        return SteerPlan(
                immediateSteer,
                Route(strikePoint.gameTime, workBackwards = true)
                        .withPart(AccelerationRoutePart(
                                flatPosition,
                                tangentPoint,
                                Duration.between(car.time, strikePoint.gameTime - turnDuration)))
                        .withPart(CircleRoutePart(
                                start = tangentPoint,
                                end = targetPosition,
                                duration = turnDuration,
                                circle = circle,
                                clockwise = clockwise)))
    }

    fun getPlanForCircleTurn(car: CarData, distancePlot: DistancePlot, flatten: Vector2, facing: Vector2): SteerPlan {
        return getPlanForCircleTurn(car, distancePlot, StrikePoint(flatten, facing, car.time))
    }
}
