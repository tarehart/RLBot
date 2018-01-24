package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

import java.awt.*
import java.util.Optional

class DribbleStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {

        val car = input.myCarData

        if (!canDribble(input, true)) {
            return Optional.empty()
        }

        val myPositonFlat = car.position.flatten()
        val myDirectionFlat = car.orientation.noseVector.flatten()
        val ballPositionFlat = input.ballPosition.flatten()
        val ballVelocityFlat = input.ballVelocity.flatten()
        val toBallFlat = ballPositionFlat.minus(myPositonFlat)
        val flatDistance = toBallFlat.magnitude()

        val ballSpeed = ballVelocityFlat.magnitude()
        val leadSeconds = .2

        val ballPath = ArenaModel.predictBallPath(input)

        val motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1)
        if (motionAfterWallBounce.isPresent && Duration.between(input.time, motionAfterWallBounce.get().time).seconds < 1) {
            return Optional.empty() // The dribble step is not in the business of wall reads.
        }

        val futureBallPosition: Vector2
        val (space) = ballPath.getMotionAt(input.time.plusSeconds(leadSeconds)).get()
        futureBallPosition = space.flatten()


        val scoreLocation = GoalUtil.getEnemyGoal(input.team).getNearestEntrance(input.ballPosition, 3.0).flatten()

        val ballToGoal = scoreLocation.minus(futureBallPosition)
        val pushDirection: Vector2
        val pressurePoint: Vector2
        var approachDistance = 0.0

        if (ballSpeed > 20) {
            val velocityCorrectionAngle = ballVelocityFlat.correctionAngle(ballToGoal)
            val angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * ballSpeed / 10))
            pushDirection = VectorUtil.rotateVector(ballToGoal, angleTweak).normalized()
            approachDistance = VectorUtil.project(toBallFlat, Vector2(pushDirection.y, -pushDirection.x)).magnitude() * 1.6 + .8
            approachDistance = Math.min(approachDistance, 4.0)
            pressurePoint = futureBallPosition.minus(pushDirection.normalized().scaled(approachDistance))
        } else {
            pushDirection = ballToGoal.normalized()
            pressurePoint = futureBallPosition.minus(pushDirection)
        }


        val carToPressurePoint = pressurePoint.minus(myPositonFlat)
        val carToBall = futureBallPosition.minus(myPositonFlat)

        val hurryUp = input.time.plusSeconds(leadSeconds)

        val hasLineOfSight = pushDirection.normalized().dotProduct(carToBall.normalized()) > -.2 || input.ballPosition.z > 2
        if (!hasLineOfSight) {
            // Steer toward a farther-back waypoint.
            val (x, y) = VectorUtil.orthogonal(pushDirection) { v -> v.dotProduct(ballToGoal) < 0 }.scaledToMagnitude(5.0)

            return Optional.of(SteerUtil.getThereOnTime(car, SpaceTime(Vector3(x, y, 0.0), hurryUp)))
        }

        val dribble = SteerUtil.getThereOnTime(car, SpaceTime(Vector3(pressurePoint.x, pressurePoint.y, 0.0), hurryUp))
        if (carToPressurePoint.normalized().dotProduct(ballToGoal.normalized()) > .80 &&
                flatDistance > 3 && flatDistance < 5 && input.ballPosition.z < 2 && approachDistance < 2
                && Vector2.angle(myDirectionFlat, carToPressurePoint) < Math.PI / 12) {
            if (car.boost > 0) {
                dribble.withAcceleration(1.0).withBoost()
            } else {
                return startPlan(SetPieces.frontFlip(), input)
            }
        }
        return Optional.of(dribble)
    }

    override fun getLocalSituation(): String {
        return "Dribbling"
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {

        val DRIBBLE_DISTANCE = 20.0

        fun canDribble(input: AgentInput, log: Boolean): Boolean {

            val car = input.myCarData
            val ballToMe = car.position.minus(input.ballPosition)

            if (ballToMe.magnitude() > DRIBBLE_DISTANCE) {
                // It got away from us
                if (log) {
                    BotLog.println("Too far to dribble", input.playerIndex)
                }
                return false
            }

            if (input.ballPosition.minus(car.position).normaliseCopy().dotProduct(
                            GoalUtil.getOwnGoal(input.team).center.minus(input.ballPosition).normaliseCopy()) > .9) {
                // Wrong side of ball
                if (log) {
                    BotLog.println("Wrong side of ball for dribble", input.playerIndex)
                }
                return false
            }

            if (VectorUtil.flatDistance(car.velocity, input.ballVelocity) > 30) {
                if (log) {
                    BotLog.println("Velocity too different to dribble.", input.playerIndex)
                }
                return false
            }

            if (BallPhysics.getGroundBounceEnergy(input.ballPosition.z, input.ballVelocity.z) > 50) {
                if (log) {
                    BotLog.println("Ball bouncing too hard to dribble", input.playerIndex)
                }
                return false
            }

            if (car.position.z > 5) {
                if (log) {
                    BotLog.println("Car too high to dribble", input.playerIndex)
                }
                return false
            }

            return true
        }
    }
}
