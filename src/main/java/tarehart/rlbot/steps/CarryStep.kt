package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration

import tarehart.rlbot.tuning.BotLog.println

/**
 * I don't think this was ever tested.
 */
class CarryStep : StandardStep() {

    override val situation: String
        get() = "Carrying"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        if (!canCarry(bundle, true)) {
            return null
        }

        val ballVelocityFlat = bundle.ballVelocity.flatten()
        val leadSeconds = .2

        val ballPath = ArenaModel.predictBallPath(bundle)

        val motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1)
        motionAfterWallBounce?.time?.let {
            if (Duration.between(bundle.time, it).seconds < 1) return null // The carry step is not in the business of wall reads.
        }

        val futureBallPosition = ballPath.getMotionAt(bundle.time.plusSeconds(leadSeconds))?.space?.flatten() ?: return null

        val scoreLocation = GoalUtil.getEnemyGoal(bundle.team).getNearestEntrance(bundle.ballPosition, 3.0).flatten()

        val ballToGoal = scoreLocation.minus(futureBallPosition)
        val pushDirection: Vector2
        val pressurePoint: Vector2
        val approachDistance = 1.0
        // TODO: vary the approachDistance based on whether the ball is forward / off to the side.

        val velocityCorrectionAngle = ballVelocityFlat.correctionAngle(ballToGoal)
        val angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * 2))
        pushDirection = VectorUtil.rotateVector(ballToGoal, angleTweak).normalized()
        pressurePoint = futureBallPosition.minus(pushDirection.scaled(approachDistance))


        val hurryUp = bundle.time.plusSeconds(leadSeconds)

        return SteerUtil.getThereOnTime(bundle.myCarData, SpaceTime(Vector3(pressurePoint.x, pressurePoint.y, 0.0), hurryUp))
    }

    companion object {
        private val MAX_X_DIFF = 1.3
        private val MAX_Y = 1.5
        private val MIN_Y = -0.9

        private fun positionInCarCoordinates(car: CarData, worldPosition: Vector3): Vector3 {
            // We will assume that the car is flat on the ground.

            // We will treat (0, 1) as the car's natural orientation.
            val carYaw = Vector2(0.0, 1.0).correctionAngle(car.orientation.noseVector.flatten())

            val carToPosition = worldPosition.minus(car.position).flatten()

            val (x, y) = VectorUtil.rotateVector(carToPosition, -carYaw)

            val zDiff = worldPosition.z - car.position.z
            return Vector3(x, y, zDiff)
        }

        private fun canCarry(bundle: TacticalBundle, log: Boolean): Boolean {

            val car = input.myCarData
            val (x, y, z) = positionInCarCoordinates(car, input.ballPosition)

            val xMag = Math.abs(x)
            if (xMag > MAX_X_DIFF) {
                if (log) {
                    println("Fell off the side", input.playerIndex)
                }
                return false
            }

            if (y > MAX_Y) {
                if (log) {
                    println("Fell off the front", input.playerIndex)
                }
                return false
            }

            if (y < MIN_Y) {
                if (log) {
                    println("Fell off the back", input.playerIndex)
                }
                return false
            }

            if (z > 3) {
                if (log) {
                    println("Ball too high to carry", input.playerIndex)
                }
                return false
            }

            if (z < 1) {
                if (log) {
                    println("Ball too low to carry", input.playerIndex)
                }
                return false
            }

            if (VectorUtil.flatDistance(car.velocity, input.ballVelocity) > 10) {
                if (log) {
                    println("Velocity too different to carry.", input.playerIndex)
                }
                return false
            }


            return true
        }
    }
}
