package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println

/**
 * I don't think this was ever tested.
 */
class CarryBotStep : StandardStep() {

    override val situation: String
        get() = "Carry Boting"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val ballPath = bundle.tacticalSituation.ballPath
        val car = bundle.agentInput.myCarData
        val ballPosition = bundle.agentInput.ballPosition
        val ballVelocity = bundle.agentInput.ballVelocity


        // Can the ball be carried right now
        // This checks if the ball is on the car, getting the ball onto the car is the responsibility of another step
        if (!canCarry(bundle, true)) {
            return null
        }

        // Bail early if the ball is going to wall bounce.
        // TODO: After the carry step is refined, consider revising this. Can we fork to a catch step?
        val motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1)
        motionAfterWallBounce?.time?.let {
            if (Duration.between(bundle.agentInput.time, it).seconds < 1) return null
        }

        val ballOffset = positionInCarCoordinates(car, ballPosition)
        val ballPristine = carPositionInPristineCoordinates(ballOffset)
        val rollDistance = ballPristine.flatten().distance(Vector2.ZERO)
        val ballContact = ballPristine.z <= 1


        if (ballContact) {
            print("Contact, ")
            print(ballPristine.y)
            print(", ")

            val ballAcceleration = ballOffset.y * car.velocity.dotProduct(car.orientation.noseVector)
            print(ballAcceleration)
            print(", ")
            println(ballVelocity.dotProduct(car.orientation.noseVector))

/*
            val ballOffset2D = ballOffset.flatten()


            var desiredLeanOffset = 0.05

            var desiredBallSpeed = 25.0
            val offsetSpeedCompensation = 0.5
            var desiredOffset = 0.0

            var ballSpeed = ballVelocity.dotProduct(car.orientation.noseVector)

            /*
            if (ballPosition.y > -50) {
                desiredBallSpeed = 3.0
            }
            **/

            if (desiredBallSpeed < ballSpeed - 5) {
                desiredBallSpeed = ballSpeed - 5
            }

            val maxBallOffset = 1.3
            desiredOffset = maxBallOffset * min(1.0, (desiredBallSpeed - ballSpeed) / 2)
            val ballForwardOffset = ballOffset.dotProduct(car.orientation.noseVector)

            val offsetDelta = ballForwardOffset - desiredOffset
            println(offsetDelta)

            val carSpeed = ballSpeed * (1 + offsetSpeedCompensation * offsetDelta)

            val allowBoost = offsetDelta > maxBallOffset / 2

            val matchBallSpeed = AccelerationModel
                    .getControlsForDesiredSpeed(carSpeed, car, finesse = ControlFinesse(allowBoost = allowBoost))
                    .withSteer(0.0)

            val renderer = BotLoopRenderer.forBotLoop(bundle.agentInput.bot)
            RenderUtil.drawSphere(renderer, ballPosition + car.orientation.noseVector.scaled(maxBallOffset), 0.5, Color.ORANGE)
            RenderUtil.drawSphere(renderer, ballPosition + car.orientation.noseVector.scaled(offsetDelta), 0.5, Color.BLUE)

            return matchBallSpeed
            */
        }

        return AccelerationModel.getControlsForDesiredSpeed(ballVelocity.flatten().magnitude(), car)

        /*
        val ballVelocityFlat = bundle.agentInput.ballVelocity.flatten()
        val leadSeconds = .2

        val futureBallPosition = ballPath.getMotionAt(bundle.agentInput.time.plusSeconds(leadSeconds))?.space?.flatten() ?: return null

        val scoreLocation = GoalUtil.getEnemyGoal(bundle.agentInput.team).getNearestEntrance(bundle.agentInput.ballPosition, 3.0).flatten()

        val ballToGoal = scoreLocation.minus(futureBallPosition)
        val pushDirection: Vector2
        val pressurePoint: Vector2
        val approachDistance = 1.0
        // TODO: vary the approachDistance based on whether the ball is forward / off to the side.

        val velocityCorrectionAngle = ballVelocityFlat.correctionAngle(ballToGoal)
        val angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * 2))
        pushDirection = VectorUtil.rotateVector(ballToGoal, angleTweak).normalized()
        pressurePoint = futureBallPosition.minus(pushDirection.scaled(approachDistance))


        val hurryUp = bundle.agentInput.time.plusSeconds(leadSeconds)

        return SteerUtil.getThereOnTime(bundle.agentInput.myCarData, SpaceTime(Vector3(pressurePoint.x, pressurePoint.y, 0.0), hurryUp))
        */
        return AgentOutput()
    }

    companion object {
        private val MAX_X_DIFF = 1.3
        private val MAX_Y = 1.5
        private val MIN_Y = -0.9

        // The perfect ball position off car center of mass where the ball will be completely still
        // This is for the octane.
        val PRISTINE_CENTER = Vector3(0.0, -0.013, 2.63)

        private fun positionInCarCoordinates(car: CarData, worldPosition: Vector3): Vector3 {
            // We will assume that the car is flat on the ground.

            // We will treat NOSE as the cars natural orientation
            val carYaw =  car.orientation.noseVector.flatten().correctionAngle(car.orientation.noseVector.flatten())

            val carToPosition = worldPosition.minus(car.position).flatten()

            val (x, y) = VectorUtil.rotateVector(carToPosition, -carYaw)

            val zDiff = worldPosition.z - car.position.z
            return Vector3(x, y, zDiff)
        }

        private fun carPositionInPristineCoordinates(carPosition: Vector3) : Vector3 {
            return carPosition - PRISTINE_CENTER
        }

        fun canCarry(bundle: TacticalBundle, log: Boolean): Boolean {

            val car = bundle.agentInput.myCarData
            val localPos = positionInCarCoordinates(car, bundle.agentInput.ballPosition)
            val y = localPos.y
            val z = localPos.z

            val xMag = Math.abs(localPos.x)
            if (xMag > MAX_X_DIFF) {
                if (log) {
                    println("Fell off the side", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (y > MAX_Y) {
                if (log) {
                    println("Fell off the front", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (y < MIN_Y) {
                if (log) {
                    println("Fell off the back", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (z > 5) {
                if (log) {
                    println("Ball too high to carry", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (z < 1) {
                if (log) {
                    println("Ball too low to carry", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (VectorUtil.flatDistance(car.velocity, bundle.agentInput.ballVelocity) > 10) {
                if (log) {
                    println("Velocity too different to carry.", bundle.agentInput.playerIndex)
                }
                return false
            }


            return true
        }
    }
}
