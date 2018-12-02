package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.intercept.strike.FlipHitStrike
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import java.awt.Graphics2D

class DribbleStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        if (!canDribble(bundle, true)) {
            return null
        }

        val myPositonFlat = car.position.flatten()
        val myDirectionFlat = car.orientation.noseVector.flatten()
        val ballPositionFlat = bundle.agentInput.ballPosition.flatten()
        val ballVelocityFlat = bundle.agentInput.ballVelocity.flatten()
        val toBallFlat = ballPositionFlat.minus(myPositonFlat)
        val flatDistance = toBallFlat.magnitude()

        val ballSpeed = ballVelocityFlat.magnitude()
        val leadSeconds = .2
        val ballPath = bundle.tacticalSituation.ballPath

        val motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1)
        motionAfterWallBounce?.time?.let {
            if (Duration.between(bundle.agentInput.time, it).seconds < 1) return null // The carry step is not in the business of wall reads.
        }

        val futureBallPosition = ballPath.getMotionAt(bundle.agentInput.time.plusSeconds(leadSeconds))?.space?.flatten() ?: return null


        val scoreLocation = GoalUtil.getEnemyGoal(bundle.agentInput.team).getNearestEntrance(bundle.agentInput.ballPosition, 3.0).flatten()

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

        val hurryUp = bundle.agentInput.time.plusSeconds(leadSeconds)

        val hasLineOfSight = pushDirection.normalized().dotProduct(carToBall.normalized()) > -.2 || bundle.agentInput.ballPosition.z > 2
        if (!hasLineOfSight) {
            // Steer toward a farther-back waypoint.
            val (x, y) = VectorUtil.orthogonal(pushDirection) { v -> v.dotProduct(ballToGoal) < 0 }.scaledToMagnitude(5.0)

            return SteerUtil.getThereOnTime(car, SpaceTime(Vector3(x, y, 0.0), hurryUp))
        }

        val dribble = SteerUtil.getThereOnTime(car, SpaceTime(Vector3(pressurePoint.x, pressurePoint.y, 0.0), hurryUp))
        if (carToPressurePoint.normalized().dotProduct(ballToGoal.normalized()) > .80 &&
                flatDistance > 3 && flatDistance < 5 && bundle.agentInput.ballPosition.z < 2 && approachDistance < 2
                && Vector2.angle(myDirectionFlat, carToPressurePoint) < Math.PI / 12) {
            if (car.boost > 0) {
                dribble.withThrottle(1.0).withBoost()
            } else {
                return startPlan(FlipHitStrike.frontFlip(), bundle)
            }
        }
        return dribble
    }

    override fun getLocalSituation(): String {
        return "Dribbling"
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {

        val DRIBBLE_DISTANCE = 20.0

        fun canDribble(bundle: TacticalBundle, log: Boolean): Boolean {

            val car = bundle.agentInput.myCarData
            val ballToMe = car.position.minus(bundle.agentInput.ballPosition)

            if (ballToMe.magnitude() > DRIBBLE_DISTANCE) {
                // It got away from us
                if (log) {
                    BotLog.println("Too far to dribble", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (bundle.agentInput.ballPosition.minus(car.position).normaliseCopy().dotProduct(
                            GoalUtil.getOwnGoal(bundle.agentInput.team).center.minus(bundle.agentInput.ballPosition).normaliseCopy()) > .9) {
                // Wrong side of ball
                if (log) {
                    BotLog.println("Wrong side of ball for dribble", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (VectorUtil.flatDistance(car.velocity, bundle.agentInput.ballVelocity) > 30) {
                if (log) {
                    BotLog.println("Velocity too different to dribble.", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (BallPhysics.getGroundBounceEnergy(bundle.agentInput.ballPosition.z, bundle.agentInput.ballVelocity.z) > 50) {
                if (log) {
                    BotLog.println("Ball bouncing too hard to dribble", bundle.agentInput.playerIndex)
                }
                return false
            }

            if (car.position.z > 5) {
                if (log) {
                    BotLog.println("Car too high to dribble", bundle.agentInput.playerIndex)
                }
                return false
            }

            return true
        }
    }
}
