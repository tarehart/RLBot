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
import tarehart.rlbot.ui.DisplayFlags
import java.awt.Color
import java.util.*

object SteerUtil {

    private const val GOOD_ENOUGH_ANGLE = 0.1F

    fun getCatchOpportunity(carData: CarData, ballPath: BallPath, boostBudget: Float): SpaceTime? {

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

    private fun canGetUnder(carData: CarData, spaceTime: SpaceTime, boostBudget: Float): Boolean {
        val plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4.0), boostBudget, carData.position.distance(spaceTime.space))

        val strikeProfile = ChipStrike()
        val orientDuration = AccelerationModel.getOrientDuration(carData, spaceTime.space)
        val dts = plot.getMotionAfterDuration(
                Duration.between(carData.time, spaceTime.time) - orientDuration,
                strikeProfile)

        val requiredDistance = SteerUtil.getDistanceFromCar(carData, spaceTime.space)
        return dts?.takeIf { it.distance > requiredDistance } != null
    }

    fun getCorrectionAngleRad(carData: CarData, target: Vector3): Float {
        return getCorrectionAngleRad(carData, target.flatten())
    }

    fun getCorrectionAngleRad(carData: CarData, target: Vector2): Float {
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
            if(DisplayFlags[DisplayFlags.GOAL_CROSSING] == 1) {
                RenderUtil.drawSquare(car.renderer, Plane(Vector3.UP, adjustedPosition.toVector3()), 1, Color.RED)
            }

        } else {
            adjustedPosition =
                    if (!detourForBoost || car.boost > 99.0) position // This keeps us from swerving all around during unlimited boost games
                    else BoostAdvisor.getBoostWaypoint(car, position) ?: position
        }

        val correctionAngle = getCorrectionAngleRad(car, adjustedPosition)
        val distance = adjustedPosition.distance(myPositionFlat)
        val speed = ManeuverMath.forwardSpeed(car)
        return getSteeringOutput(correctionAngle, distance, speed, car.isSupersonic, conserveBoost, car.spin.yawRate)
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
        return nearestGoal.getNearestEntrance(desiredExit.toVector3(), 2F).flatten()
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

    fun steerTowardPositionAcrossSeam(car: CarData, position: Vector3): AgentOutput {

        val carPlane = ArenaModel.getNearestPlane(car.position)
        val targetPlane = ArenaModel.getNearestPlane(position)

        if (carPlane.normal.z == 1F &&
                TacticsTelemetry[car.playerIndex]?.gameMode == GameMode.SOCCER &&
                crossesSoccerGoalLine(position.flatten(), car.position.flatten())) {

            return steerTowardGroundPosition(car, position) // Avoid the goal posts!
        }

        if (carPlane.normal == targetPlane.normal) {
            return steerTowardWallPosition(car, position)
        }

        val toPositionOnTargetPlane = (position - car.position).projectToPlane(targetPlane.normal)
        val carShadowOnTargetPlane = car.position.shadowOntoPlane(targetPlane)
        val distanceFromTargetPlane = targetPlane.distance(car.position)
        val targetDistanceFromCarPlane =  carPlane.distance(position)
        val hurryToSeamBias = 1.5F // 1.0 would be neutral
        val carPlaneWeight = distanceFromTargetPlane / (distanceFromTargetPlane + targetDistanceFromCarPlane * hurryToSeamBias)
        val toPositionAlongSeam = toPositionOnTargetPlane.projectToPlane(carPlane.normal)
        val seamPosition = carShadowOnTargetPlane.plus(toPositionAlongSeam.scaled(carPlaneWeight))

        return steerTowardWallPosition(car, seamPosition)
    }

    fun steerTowardWallPosition(carData: CarData, position: Vector3): AgentOutput {
        val toPosition = position.minus(carData.position)
        val correctionAngle = VectorUtil.getCorrectionAngle(carData.orientation.noseVector, toPosition, carData.orientation.roofVector)
        val speed = carData.velocity.magnitude()
        val distance = position.distance(carData.position)
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic, false, carData.spin.yawRate)
    }

    /**
     * Before changing this, note that it's linked to getSteerPenaltySeconds, which was tuned via lots of
     * timing trials to predict time-to-target. If you mess with this, you should probably redo that via
     * DriveToPositionSampler.
     */
    private fun getSteeringOutput(correctionAngle: Float, distance: Float, speed: Float, isSupersonic: Boolean, conserveBoost: Boolean, yawRate: Float): AgentOutput {
        val difference = Math.abs(correctionAngle)
        val turnSharpness = difference * 6 / Math.PI + difference * speed * .1
        //turnSharpness = (1 - DEAD_ZONE) * turnSharpness + Math.signum(turnSharpness) * DEAD_ZONE;

        val yawRateVsIntent = yawRate * -Math.signum(correctionAngle)

        val shouldBrake = distance < 25 && difference > Math.PI / 4 && speed > 25 || speed > 20 && difference > Math.PI / 2
        val shouldSlide = yawRateVsIntent > 1 &&
                (speed < 30 && distance < 15 && difference > Math.PI / 2 || speed < 30 && difference > 3 * Math.PI / 4)
        val shouldBoost = !conserveBoost && !shouldBrake && difference < Math.PI / 6 && !isSupersonic

        return AgentOutput()
                .withThrottle(if (shouldBrake) -1.0 else 1.0)
                .withSteer(-Math.signum(correctionAngle) * turnSharpness)
                .withSlide(shouldSlide)
                .withBoost(shouldBoost)
    }

    /**
     * Based on the standard steering algorithm, how much time are we going to burn changing direction?
     * Steer penalty seconds is typically added to the idealized 1D acceleration model.
     *
     * This was tuned via lots of timing trials using DriveToPositionSampler to predict time-to-target.
     */
    fun getSteerPenaltySeconds(car: CarData, target: Vector3): Float {
        val unpleasantSpeed = car.velocity.magnitude() - car.velocity.dotProduct((target - car.position).normaliseCopy())
        val toTargetOnPlane = (target - car.position).projectToPlane(car.orientation.roofVector).normaliseCopy()
        val angle = toTargetOnPlane.angle(car.orientation.noseVector)

        return 0.2786F * angle +
               -0.101F  * angle * angle +
               0.0326F * angle * angle * angle +
               0.0151F * unpleasantSpeed
    }

    fun steerTowardGroundPosition(carData: CarData, position: Vector3): AgentOutput {
        return steerTowardGroundPosition(carData, position.flatten(), detourForBoost = false)
    }

    fun getDistanceFromCar(car: CarData, loc: Vector3): Float {
        return VectorUtil.flatDistance(loc, car.position)
    }

    fun getSensibleFlip(car: CarData, target: Vector3): Plan? {
        return getSensibleFlip(car, target.flatten())
    }

    fun getSensibleFlip(car: CarData, target: Vector2, maximumSafeFlipDistanceOverride: Float? = null): Plan? {

        if (!car.hasWheelContact || car.position.z > ManeuverMath.BASE_CAR_Z ||
                car.orientation.roofVector.dotProduct(Vector3(0.0, 0.0, 1.0)) < .98) {
            return null
        }

        if (ArenaModel.isMicroGravity()) {
            return null // Flipping in microgravity is a bad idea!
        }

        val toTarget = target.minus(car.position.flatten())
        val targetDistance = maximumSafeFlipDistanceOverride ?: toTarget.magnitude()
        val toTargetAngle = Vector2.angle(car.orientation.noseVector.flatten(), toTarget)
        val speed = car.velocity.flatten().magnitude()

        if (targetDistance > 40 &&
                toTargetAngle > 3 * Math.PI / 4 &&
                (car.velocity.flatten().dotProduct(toTarget) > 0 || speed < 5)) {

            return SetPieces.halfFlip(target)
        }

        if (targetDistance > 20 &&
                toTargetAngle > Math.PI / 2 &&
                Vector2.angle(car.velocity.flatten(), toTarget) < Math.PI / 12) {

            return SetPieces.anyDirectionFlip(car, toTarget)
        }

        if (car.isSupersonic || car.boost > 75 || speed < AccelerationModel.FLIP_THRESHOLD_SPEED) {
            return null
        }

        val distanceCovered = AccelerationModel.getFrontFlipDistance(speed)
        // TODO: isDrivingOnTarget is often false in situations where the bot is greedy for boost, because the
        // steering gets overridden. Consider making the greed a pre-target computation more often.
        if (targetDistance > distanceCovered + 10 && isDrivingOnTarget(car, target)) {
            return SetPieces.speedupFlip()
        }

        return null
    }

    fun isDrivingOnTarget(car: CarData, target: Vector2): Boolean {
        val toTarget = target.minus(car.position.flatten())
        val facing = car.orientation.noseVector.flatten()
        val facingCorrection = facing.correctionAngle(toTarget)
        val slideAngle = facing.correctionAngle(car.velocity.flatten())
        return Math.abs(facingCorrection) < GOOD_ENOUGH_ANGLE && Math.abs(slideAngle) < GOOD_ENOUGH_ANGLE
    }

    fun getThereOnTime(car: CarData, groundPositionAndTime: SpaceTime, greedy: Boolean = false): AgentOutput {



        var timeToIntercept = Duration.between(car.time, groundPositionAndTime.time)
        if (timeToIntercept.millis < 0) {
            timeToIntercept = Duration.ofMillis(1)
        }

        var waypoint = groundPositionAndTime.space.flatten()
        val distanceToIntercept = car.position.flatten().distance(waypoint)
        val averageSpeedNeeded = distanceToIntercept / timeToIntercept.seconds

        val distancePlot = AccelerationModel.simulateAcceleration(car, groundPositionAndTime.time - car.time, 0F, 0)

        if (distancePlot.getEndPoint().distance > distanceToIntercept + 15) {
            // Go backwards
            return AgentOutput().withThrottle(-1)
        }

        if (greedy && distanceToIntercept > 40 && car.boost < 75) {
            waypoint = Optional.ofNullable(BoostAdvisor.getBoostWaypoint(car, waypoint)).orElse(waypoint)
        }

        val agentOutput = SteerUtil.steerTowardGroundPosition(car, waypoint, detourForBoost = false)
                .withBoost(false)
                .withThrottle(AccelerationModel.getThrottleForDesiredSpeed(averageSpeedNeeded, car))

        return agentOutput
    }

}
