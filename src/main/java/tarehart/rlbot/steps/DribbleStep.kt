package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.strike.DribbleStrike
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import java.awt.Color
import java.awt.Graphics2D

class DribbleStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val renderer = car.renderer

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(3.0), car.boost)

        val ballPath = bundle.tacticalSituation.ballPath
        val ballPosition = bundle.agentInput.ballPosition

        val ballToCar = car.position - ballPosition

        val intercept = InterceptCalculator.getFilteredInterceptOpportunity(
                car,
                ballPath,
                distancePlot,
                interceptModifier = ballToCar.scaledToMagnitude(1.5),
                predicate =  {_, st -> st.space.z < 2},
                strikeProfileFn =  { DribbleStrike() }) ?: return null

        renderer.drawCenteredRectangle3d(Color.CYAN, intercept.space.toRlbot(), 4, 4, true)
        renderer.drawLine3d(Color.GREEN, car.position.toRlbot(), intercept.space.toRlbot())

        val enemyGoal = GoalUtil.getEnemyGoal(car.team)
        val ballToGoal = enemyGoal.getNearestEntrance(ballPosition, 3.0).minus(ballPosition).flatten()
        val ballCorrectionRadians = bundle.agentInput.ballVelocity.flatten().correctionAngle(ballToGoal)
        val approachFromLeft = ballCorrectionRadians < 0

        val carToIntercept = intercept.space.minus(car.position).flatten()
        val carCorrectionRadians = car.orientation.noseVector.flatten().correctionAngle(carToIntercept)

        val distanceToIntercept = carToIntercept.magnitude()
        val magnitude =
                if (carCorrectionRadians * ballCorrectionRadians < 0)
                    // We're on the wrong side of the ball, get over quick!
                    1.2
                else
                    Math.min(0.2 + distanceToIntercept / 4, BASIS_SIZE)
        val approachBasis = VectorUtil.orthogonal(carToIntercept, approachFromLeft).scaledToMagnitude(magnitude)
        val ballHeightFallback = (ballPosition.z - ArenaModel.BALL_RADIUS) * 0.5
        val basisTip = intercept.space
                .plus(approachBasis.withZ(0.0))
                .minus(carToIntercept.scaledToMagnitude(ballHeightFallback).withZ(0.0))

        renderer.drawLine3d(Color.ORANGE, intercept.space.toRlbot(), basisTip.toRlbot())

        renderer.drawCenteredRectangle3d(Color.RED, basisTip.toRlbot(), 8, 8, true)
        val speedupMillis = if (distanceToIntercept > 5) 100L else 0
        return SteerUtil.getThereOnTime(car, SpaceTime(basisTip, intercept.time - Duration.ofMillis(speedupMillis)))
    }

    override fun getLocalSituation(): String {
        return "Dribbling"
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {

        const val BASIS_SIZE = 5.0
        private const val MAX_DRIBBLE_DISTANCE = 50.0

        fun canDribble(bundle: TacticalBundle, log: Boolean): Boolean {

            val car = bundle.agentInput.myCarData
            val ballToMe = car.position.minus(bundle.agentInput.ballPosition)

            if (ballToMe.magnitude() > MAX_DRIBBLE_DISTANCE) {
                // It got away from us
                if (log) {
                    BotLog.println("Too far to dribble", bundle.agentInput.playerIndex)
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
