package tarehart.rlbot.steps.challenge

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.defense.ThreatAssessor
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class ChallengeStep: NestedPlanStep() {

    private var originalTouch: BallTouch? = null
    private var latestDefensiveNode: Vector2? = null
    private val ballPathDisruptionMeter = BallPathDisruptionMeter()


    override fun getLocalSituation(): String {
        return  "Working on challenge"
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        return !threatExists(bundle)
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        if (ballPathDisruptionMeter.isDisrupted(bundle.tacticalSituation.ballPath)) {
            println("Ball path disrupted, quitting challenge", bundle.agentInput.playerIndex)
            return null
        }

        val tacticalSituation = bundle.tacticalSituation
        val ballAdvantage = tacticalSituation.ballAdvantage
        if (ballAdvantage.seconds > 0.5) {
            return null // We can probably go for a shot now.
        }

        val ourIntercept = bundle.tacticalSituation.expectedContact
        val enemyIntercept = bundle.tacticalSituation.enemyPlayerWithInitiative?.intercept ?: return null
        val ourTimeToEnemyIntercept = ourIntercept?.distancePlot?.getMotionUponArrival(car, enemyIntercept.space)?.time ?: return null
        val toEnemyInterceptAdvantage = enemyIntercept.time - (car.time + ourTimeToEnemyIntercept)

        val enemyContact = tacticalSituation.expectedEnemyContact ?:
            return null

        val enemyShotLine = GoalUtil.getOwnGoal(bundle.agentInput.team).center - enemyContact.space

        val flatPosition = car.position.flatten()
        val defensiveNode = ArenaModel.clampPosition(enemyContact.space.flatten() + enemyShotLine.flatten().scaledToMagnitude(DEFENSIVE_NODE_DISTANCE), 3.0)

        latestDefensiveNode = defensiveNode

        val defensiveNodeDistance = flatPosition.distance(defensiveNode)

        if (tacticalSituation.distanceBallIsBehindUs > 0 && ballAdvantage.seconds > -.2) {
            startPlan(
                    Plan(Posture.DEFENSIVE)
                            .withStep(FlexibleKickStep(KickAwayFromOwnGoal())),
                    bundle)
        }

        if (defensiveNodeDistance < DEFENSIVE_NODE_DISTANCE + 15 && ballAdvantage.seconds > -.3) { // Don't set ball advantage too low or you'll break kickoffs.
            startPlan(
                    Plan(Posture.DEFENSIVE)
                            .withStep(InterceptStep(enemyShotLine.scaledToMagnitude(1.5))),
                    bundle)
        }

        SteerUtil.getSensibleFlip(car, defensiveNode)?.let {
            if (car.boost < 1 && tacticalSituation.distanceBallIsBehindUs > 0) { // Use more boost and less flipping during challenges.
                return startPlan(it, bundle)
            }
        }

        val renderer = car.renderer
        RenderUtil.drawSquare(renderer, Plane(enemyShotLine.normaliseCopy(), enemyContact.space), 1.0, Color(0.8f, 0.0f, 0.8f))
        RenderUtil.drawSquare(renderer, Plane(enemyShotLine.normaliseCopy(), enemyContact.space), 1.5, Color(0.8f, 0.0f, 0.8f))
        RenderUtil.drawSquare(renderer, Plane(enemyShotLine.normaliseCopy(), defensiveNode.withZ(1.0)), 1.5, Color.MAGENTA)

        // If we're too greedy, we'll be late to kickoffs
        return SteerUtil.steerTowardGroundPosition(car, defensiveNode,
                detourForBoost = defensiveNodeDistance > 50 && car.boost < 50)
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        super.drawDebugInfo(graphics)

        latestDefensiveNode?.let {
            graphics.color = Color(73, 111, 73)
            graphics.stroke = BasicStroke(1f)

            val (x, y) = it
            val crossSize = 2
            graphics.draw(Line2D.Double(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
            graphics.draw(Line2D.Double(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
        }
    }

    companion object {

        const val DEFENSIVE_NODE_DISTANCE = 18.0
        const val RISKIEST_CHALLENGE_ADVANTAGE_SECONDS = -0.2


        fun threatExists(bundle: TacticalBundle): Boolean {
            val threatReport = ThreatAssessor.getThreatReport(bundle)
            return threatReport.challengeImminent
        }
    }
}
