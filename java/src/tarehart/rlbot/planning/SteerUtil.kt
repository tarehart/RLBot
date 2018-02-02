package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BoostData
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.*
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.time.Duration

import java.util.Optional

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

        val dts = plot.getMotionAfterDuration(
                carData,
                spaceTime.space,
                Duration.between(carData.time, spaceTime.time),
                StrikeProfile())

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

    fun steerTowardGroundPosition(carData: CarData, position: Vector2): AgentOutput {

        if (ArenaModel.isCarOnWall(carData)) {
            return steerTowardGroundPositionFromWall(carData, position)
        }

        val correctionAngle = getCorrectionAngleRad(carData, position)
        val myPositionFlat = carData.position.flatten()
        val distance = position.distance(myPositionFlat)
        val speed = carData.velocity.magnitude()
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic, false)
    }

    fun steerTowardGroundPosition(carData: CarData, position: Vector2, noBoosting: Boolean): AgentOutput {

        if (ArenaModel.isCarOnWall(carData)) {
            return steerTowardGroundPositionFromWall(carData, position)
        }

        val correctionAngle = getCorrectionAngleRad(carData, position)
        val myPositionFlat = carData.position.flatten()
        val distance = position.distance(myPositionFlat)
        val speed = carData.velocity.magnitude()
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic, noBoosting)
    }

    fun steerTowardGroundPosition(carData: CarData, boostData: BoostData, position: Vector2): AgentOutput {

        if (ArenaModel.isCarOnWall(carData)) {
            return steerTowardGroundPositionFromWall(carData, position)
        }

        val adjustedPosition = Optional.ofNullable(BoostAdvisor.getBoostWaypoint(carData, boostData, position)).orElse(position)

        val correctionAngle = getCorrectionAngleRad(carData, adjustedPosition)
        val myPositionFlat = carData.position.flatten()
        val distance = adjustedPosition.distance(myPositionFlat)
        val speed = carData.velocity.magnitude()
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic, false)
    }

    fun backUpTowardGroundPosition(car: CarData, position: Vector2): AgentOutput {

        val positionToCar = car.position.flatten() - position
        val correctionRadians = car.orientation.noseVector.flatten().correctionAngle(positionToCar)
        val correctionDirection = Math.signum(correctionRadians)

        val difference = Math.abs(correctionRadians)
        val turnSharpness = difference * 2

        return AgentOutput()
                .withDeceleration(1.0)
                .withSteer(correctionDirection * turnSharpness)
                .withSlide(Math.abs(correctionRadians) > Math.PI / 3)
    }

    private fun steerTowardGroundPositionFromWall(carData: CarData, position: Vector2): AgentOutput {
        val toPositionFlat = position.minus(carData.position.flatten())
        val carShadow = Vector3(carData.position.x, carData.position.y, 0.0)
        val heightOnWall = carData.position.z
        val wallNormal = carData.orientation.roofVector
        val distanceOntoField = VectorUtil.project(toPositionFlat, wallNormal.flatten()).magnitude()
        val wallWeight = heightOnWall / (heightOnWall + distanceOntoField)
        val toPositionAlongSeam = Vector3(toPositionFlat.x, toPositionFlat.y, 0.0).projectToPlane(wallNormal)
        val seamPosition = carShadow.plus(toPositionAlongSeam.scaled(wallWeight))

        return steerTowardWallPosition(carData, seamPosition)
    }

    fun steerTowardWallPosition(carData: CarData, position: Vector3): AgentOutput {
        val toPosition = position.minus(carData.position)
        val correctionAngle = VectorUtil.getCorrectionAngle(carData.orientation.noseVector, toPosition, carData.orientation.roofVector)
        val speed = carData.velocity.magnitude()
        val distance = position.distance(carData.position)
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic, false)
    }

    private fun getSteeringOutput(correctionAngle: Double, distance: Double, speed: Double, isSupersonic: Boolean, noBoosting: Boolean): AgentOutput {
        val difference = Math.abs(correctionAngle)
        val turnSharpness = difference * 6 / Math.PI + difference * speed * .1
        //turnSharpness = (1 - DEAD_ZONE) * turnSharpness + Math.signum(turnSharpness) * DEAD_ZONE;

        val shouldBrake = distance < 25 && difference > Math.PI / 4 && speed > 25
        val shouldSlide = shouldBrake || difference > Math.PI / 2
        val shouldBoost = !noBoosting && !shouldBrake && difference < Math.PI / 6 && !isSupersonic

        return AgentOutput()
                .withAcceleration((if (shouldBrake) 0 else 1).toDouble())
                .withDeceleration((if (shouldBrake) 1 else 0).toDouble())
                .withSteer((-Math.signum(correctionAngle) * turnSharpness).toFloat().toDouble())
                .withSlide(shouldSlide)
                .withBoost(shouldBoost)
    }

    fun steerTowardGroundPosition(carData: CarData, position: Vector3): AgentOutput {
        return steerTowardGroundPosition(carData, position.flatten())
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
        if (distanceToIntercept > distanceCovered + 10) {

            val facing = car.orientation.noseVector.flatten()
            val facingCorrection = facing.correctionAngle(toTarget)
            val slideAngle = facing.correctionAngle(car.velocity.flatten())

            if (Math.abs(facingCorrection) < GOOD_ENOUGH_ANGLE && Math.abs(slideAngle) < GOOD_ENOUGH_ANGLE) {
                return SetPieces.frontFlip()
            }
        }

        return null
    }

    fun getThereOnTime(car: CarData, groundPositionAndTime: SpaceTime): AgentOutput {

        var timeToIntercept = Duration.between(car.time, groundPositionAndTime.time)
        if (timeToIntercept.millis < 0) {
            timeToIntercept = Duration.ofMillis(1)
        }
        val distancePlot = AccelerationModel.simulateAcceleration(
                car, timeToIntercept, car.boost, 0.0)
        val dts = distancePlot.getEndPoint()
        val maxDistance = dts.distance

        val distanceToIntercept = car.position.flatten().distance(groundPositionAndTime.space.flatten())
        val distanceRatio = maxDistance / distanceToIntercept
        val averageSpeedNeeded = distanceToIntercept / timeToIntercept.seconds
        val currentSpeed = car.velocity.magnitude()

        val agentOutput = SteerUtil.steerTowardGroundPosition(car, groundPositionAndTime.space.flatten())
        if (distanceRatio > 1.1) {
            agentOutput.withBoost(false)
            if (currentSpeed > averageSpeedNeeded) {
                // Slow down
                agentOutput.withAcceleration(0.0).withBoost(false).withDeceleration(Math.max(0.0, distanceRatio - 1.5)) // Hit the brakes, but keep steering!
                if (car.orientation.noseVector.dotProduct(car.velocity) < 0) {
                    // car is going backwards
                    agentOutput.withDeceleration(0.0).withSteer(0.0)
                }
            } else if (distanceRatio > 1.5) {
                agentOutput.withAcceleration(.5)
            }
        }

        if (currentSpeed > averageSpeedNeeded) {
            agentOutput.withBoost(false)
            agentOutput.withAcceleration(averageSpeedNeeded / currentSpeed)
        }
        return agentOutput
    }

}
