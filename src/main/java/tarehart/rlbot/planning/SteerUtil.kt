package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.ChipStrike
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color
import java.util.*

object SteerUtil {

    private val GOOD_ENOUGH_ANGLE = Math.PI / 12
    private val DEAD_ZONE = 0.0

    fun getCatchOpportunity(carData: CarData, ballPath: BallPath, boostBudget: Double): SpaceTime? {

        var searchStart = carData.time

        val groundBounceEnergy = BallPhysics.getGroundBounceEnergy(ballPath.startPoint.space.z, ballPath.startPoint.velocity.z)

        if (groundBounceEnergy < 50) {
            return null
        }

        for (i in 0..2) {
            val landingOption = ballPath.getLanding(searchStart)

            if (landingOption != null) {
                val landing = landingOption.toSpaceTime()
                if (canGetUnder(carData, landing, boostBudget)) {
                    return landing
                } else {
                    searchStart = landing.time.plusSeconds(1.0)
                }
            } else {
                return null
            }
        }

        return null
    }

    private fun canGetUnder(carData: CarData, spaceTime: SpaceTime, boostBudget: Double): Boolean {
        val plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4.0), boostBudget, carData.position.distance(spaceTime.space))

        val strikeProfile = ChipStrike()
        val orientDuration = AccelerationModel.getOrientDuration(carData, spaceTime.space)
        val dts = plot.getMotionAfterDuration(
                Duration.between(carData.time, spaceTime.time) - orientDuration,
                strikeProfile)

        val requiredDistance = SteerUtil.getDistanceFromCar(carData, spaceTime.space)
        return dts?.takeIf { it.distance > requiredDistance } != null
    }

    fun getCorrectionAngleRad(carData: CarData, target: Vector3): Double {
        return getCorrectionAngleRad(carData, target.flatten())
    }

    fun getCorrectionAngleRad(carData: CarData, target: Vector2): Double {
        val noseVector = carData.orientation.noseVector.flatten()
        val toTarget = target.minus(carData.position.flatten())
        return noseVector.correctionAngle(toTarget)
    }

    fun steerTowardGroundPosition(car: CarData, position: Vector2,
                                  detourForBoost: Boolean = true, conserveBoost: Boolean = false): AgentOutput {

        if (ArenaModel.isCarOnWall(car)) {
            return steerTowardPositionAcrossSeam(car, position.toVector3())
        }

        val myPositionFlat = car.position.flatten()
        val adjustedPosition: Vector2

        if (TacticsTelemetry[car.playerIndex]?.gameMode == GameMode.SOCCER &&
                crossesSoccerGoalLine(position, myPositionFlat)) {

            adjustedPosition = getAdjustedSoccerGoalCrossing(position, myPositionFlat)
            RenderUtil.drawSquare(car.renderer, Plane(Vector3.UP, adjustedPosition.toVector3()), 1.0, Color.RED)

        } else {
            adjustedPosition =
                    if (!detourForBoost || car.boost > 99.0) position // This keeps us from swerving all around during unlimited boost games
                    else Optional.ofNullable(BoostAdvisor.getBoostWaypoint(car, position)).orElse(position)
        }

        val correctionAngle = getCorrectionAngleRad(car, adjustedPosition)
        val distance = adjustedPosition.distance(myPositionFlat)
        val speed = car.velocity.magnitude()
        return getSteeringOutput(correctionAngle, distance, speed, car.isSupersonic, conserveBoost)
    }

    private fun crossesSoccerGoalLine(p1: Vector2, p2: Vector2): Boolean {
        return Math.abs(p1.y) > ArenaModel.BACK_WALL || Math.abs(p2.y) > ArenaModel.BACK_WALL &&  // Something is past the goal line
                (Math.abs(p1.y) < ArenaModel.BACK_WALL || Math.abs(p2.y) < ArenaModel.BACK_WALL) // Something is not past the goal line
    }

    /**
     * Assumes that the two positions are on opposite sides of a goal line.
     */
    private fun getAdjustedSoccerGoalCrossing(p1: Vector2, p2: Vector2): Vector2 {
        val toTarget = p1 - p2
        val yLine = Math.signum(p1.y + p2.y) * ArenaModel.BACK_WALL
        val crossingFactor = (yLine - p2.y) / (p1.y - p2.y)
        val desiredExit = p2 + toTarget * crossingFactor
        val nearestGoal = GoalUtil.getNearestGoal(desiredExit.toVector3())
        return nearestGoal.getNearestEntrance(desiredExit.toVector3(), 2.0).flatten()
    }

    fun backUpTowardGroundPosition(car: CarData, position: Vector2): AgentOutput {

        val positionToCar = car.position.flatten() - position
        val correctionRadians = car.orientation.noseVector.flatten().correctionAngle(positionToCar)
        val correctionDirection = Math.signum(correctionRadians)

        val difference = Math.abs(correctionRadians)
        val turnSharpness = difference * 2

        return AgentOutput()
                .withThrottle(-1.0)
                .withSteer(correctionDirection * turnSharpness)
                .withSlide(Math.abs(correctionRadians) > Math.PI / 3)
    }

    fun steerTowardPositionAcrossSeam(carData: CarData, position: Vector3): AgentOutput {

        val carPlane = ArenaModel.getNearestPlane(carData.position)
        val targetPlane = ArenaModel.getNearestPlane(position)

        if (carPlane.normal == targetPlane.normal) {
            return steerTowardWallPosition(carData, position)
        }

        val toPositionOnTargetPlane = (position - carData.position).projectToPlane(targetPlane.normal)
        val carShadowOnTargetPlane = carData.position.shadowOntoPlane(targetPlane)
        val distanceFromTargetPlane = targetPlane.distance(carData.position)
        val targetDistanceFromCarPlane =  carPlane.distance(position)
        val hurryToSeamBias = 1.5 // 1.0 would be neutral
        val carPlaneWeight = distanceFromTargetPlane / (distanceFromTargetPlane + targetDistanceFromCarPlane * hurryToSeamBias)
        val toPositionAlongSeam = toPositionOnTargetPlane.projectToPlane(carPlane.normal)
        val seamPosition = carShadowOnTargetPlane.plus(toPositionAlongSeam.scaled(carPlaneWeight))

        return steerTowardWallPosition(carData, seamPosition)
    }

    fun steerTowardWallPosition(carData: CarData, position: Vector3): AgentOutput {
        val toPosition = position.minus(carData.position)
        val correctionAngle = VectorUtil.getCorrectionAngle(carData.orientation.noseVector, toPosition, carData.orientation.roofVector)
        val speed = carData.velocity.magnitude()
        val distance = position.distance(carData.position)
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic, false)
    }

    private fun getSteeringOutput(correctionAngle: Double, distance: Double, speed: Double, isSupersonic: Boolean, conserveBoost: Boolean): AgentOutput {
        val difference = Math.abs(correctionAngle)
        val turnSharpness = difference * 6 / Math.PI + difference * speed * .1
        //turnSharpness = (1 - DEAD_ZONE) * turnSharpness + Math.signum(turnSharpness) * DEAD_ZONE;

        val shouldBrake = distance < 25 && difference > Math.PI / 4 && speed > 25 || speed > 20 && difference > Math.PI / 2
        val shouldSlide = speed < 30 && distance < 15 && difference > Math.PI / 4 || speed < 30 && difference > 3 * Math.PI / 4
        val shouldBoost = !conserveBoost && !shouldBrake && difference < Math.PI / 6 && !isSupersonic

        return AgentOutput()
                .withThrottle(if (shouldBrake) -1.0 else 1.0)
                .withSteer(-Math.signum(correctionAngle) * turnSharpness)
                .withSlide(shouldSlide)
                .withBoost(shouldBoost)
    }

    fun steerTowardGroundPosition(carData: CarData, position: Vector3): AgentOutput {
        return steerTowardGroundPosition(carData, position.flatten(), detourForBoost = false)
    }

    fun getDistanceFromCar(car: CarData, loc: Vector3): Double {
        return VectorUtil.flatDistance(loc, car.position)
    }

    fun getSensibleFlip(car: CarData, target: Vector3): Plan? {
        return getSensibleFlip(car, target.flatten())
    }

    fun getSensibleFlip(car: CarData, target: Vector2): Plan? {

        if (car.orientation.roofVector.dotProduct(Vector3(0.0, 0.0, 1.0)) < .98 ||
                !car.hasWheelContact) {
            return null
        }

        val toTarget = target.minus(car.position.flatten())
        if (toTarget.magnitude() > 40 &&
                Vector2.angle(car.orientation.noseVector.flatten(), toTarget) > 3 * Math.PI / 4 &&
                (car.velocity.flatten().dotProduct(toTarget) > 0 || car.velocity.magnitude() < 5)) {

            return SetPieces.halfFlip(target)
        }

        val speed = car.velocity.flatten().magnitude()
        if (car.isSupersonic || car.boost > 75 || speed < AccelerationModel.FLIP_THRESHOLD_SPEED) {
            return null
        }

        val distanceCovered = AccelerationModel.getFrontFlipDistance(speed)


        val distanceToIntercept = toTarget.magnitude()
        if (distanceToIntercept > distanceCovered + 25) {

            val facing = car.orientation.noseVector.flatten()
            val facingCorrection = facing.correctionAngle(toTarget)
            val slideAngle = facing.correctionAngle(car.velocity.flatten())

            if (Math.abs(facingCorrection) < GOOD_ENOUGH_ANGLE && Math.abs(slideAngle) < GOOD_ENOUGH_ANGLE) {
                return SetPieces.speedupFlip()
            }
        }

        return null
    }

    fun getThereOnTime(car: CarData, groundPositionAndTime: SpaceTime, greedy: Boolean = false): AgentOutput {

        var timeToIntercept = Duration.between(car.time, groundPositionAndTime.time)
        if (timeToIntercept.millis < 0) {
            timeToIntercept = Duration.ofMillis(1)
        }
        val distancePlot = AccelerationModel.simulateAcceleration(
                car, timeToIntercept, 0.0, 0.0)
        val dts = distancePlot.getEndPoint()
        val maxDistance = dts.distance

        var waypoint = groundPositionAndTime.space.flatten()
        val distanceToIntercept = car.position.flatten().distance(waypoint)
        val distanceRatio = maxDistance / distanceToIntercept
        val averageSpeedNeeded = distanceToIntercept / timeToIntercept.seconds
        val currentSpeed = ManeuverMath.forwardSpeed(car)


        if (greedy && distanceToIntercept > 40 && car.boost < 75) {
            waypoint = Optional.ofNullable(BoostAdvisor.getBoostWaypoint(car, waypoint)).orElse(waypoint)
        }

        val agentOutput = SteerUtil.steerTowardGroundPosition(car, waypoint, detourForBoost = false)

        if (distanceRatio > 2) {
            agentOutput.withThrottle(-1.0).withBoost(false).withSteer(0.0)
        } else {
            agentOutput.withBoost(distanceRatio < 0.7 && averageSpeedNeeded > currentSpeed)
            agentOutput.withThrottle(averageSpeedNeeded - currentSpeed)
        }
        return agentOutput
    }

}
