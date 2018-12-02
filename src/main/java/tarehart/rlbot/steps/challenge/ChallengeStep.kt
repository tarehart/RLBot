package tarehart.rlbot.steps.challenge

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.*
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.tactics.TacticalSituation
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class ChallengeStep: NestedPlanStep() {

    private val backoff = 15.0

    private var originalTouch: BallTouch? = null
    private var latestDefensiveNode: Vector2? = null

    override fun getLocalSituation(): String {
        return  "Working on challenge"
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        val tacticalSituation = TacticsTelemetry.get(bundle.agentInput.playerIndex) ?: return false
        return !threatExists(tacticalSituation)
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        if (originalTouch == null) {
            originalTouch = bundle.agentInput.latestBallTouch
        } else {

            if (originalTouch?.position ?: Vector3() != bundle.agentInput.latestBallTouch?.position ?: Vector3()) {
                // There has been a new ball touch.
                println("Ball has been touched, quitting challenge", bundle.agentInput.playerIndex)
                return null
            }
        }

        // TODO: Basically, destroy TacticsTelemetry because it shouldn't be needed.
        // At least don't allow steps to use it, that is bad form.
        val tacticalSituation = TacticsTelemetry.get(bundle.agentInput.playerIndex) ?: return null
        val ballAdvantage = tacticalSituation.ballAdvantage
        if (ballAdvantage.seconds > 1.0) {
            return null // We can probably go for a shot now.
        }

        val enemyContact = tacticalSituation.expectedEnemyContact ?:
            return null

        if (enemyContact.space.z > StrikePlanner.NEEDS_AERIAL_THRESHOLD) {
            return null
        }

        val enemyShotLine = GoalUtil.getOwnGoal(bundle.agentInput.team).center - enemyContact.space

        val flatPosition = car.position.flatten()
        val defensiveNode = enemyContact.space.flatten() + enemyShotLine.flatten().scaledToMagnitude(backoff)
        latestDefensiveNode = defensiveNode

        val defensiveNodeDistance = flatPosition.distance(defensiveNode)

        if (tacticalSituation.distanceBallIsBehindUs > 0 && ballAdvantage.seconds > -.2) {
            startPlan(
                    Plan(Plan.Posture.DEFENSIVE)
                            .withStep(FlexibleKickStep(KickAwayFromOwnGoal())),
                    bundle)
        }

        if (defensiveNodeDistance < backoff + 15 && ballAdvantage.seconds > -.3) { // Don't set ball advantage too low or you'll break kickoffs.
            startPlan(
                    Plan(Plan.Posture.DEFENSIVE)
                            .withStep(InterceptStep(enemyShotLine.scaledToMagnitude(1.5))),
                    bundle)
        }

        SteerUtil.getSensibleFlip(car, defensiveNode)?.let {
            if (car.boost < 1 && tacticalSituation.distanceBallIsBehindUs > 0) { // Use more boost and less flipping during challenges.
                return startPlan(it, bundle)
            }
        }

        val renderer = BotLoopRenderer.forBotLoop(bundle.agentInput.bot)
        RenderUtil.drawSquare(renderer, Plane(enemyShotLine.normaliseCopy(), enemyContact.space), 1.0, Color(0.8f, 0.0f, 0.8f))
        RenderUtil.drawSquare(renderer, Plane(enemyShotLine.normaliseCopy(), enemyContact.space), 1.5, Color(0.8f, 0.0f, 0.8f))

        return SteerUtil.steerTowardGroundPositionGreedily(car, defensiveNode)
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
        fun threatExists(tacticalSituation: TacticalSituation): Boolean {
            return tacticalSituation.ballAdvantage.seconds < 1.0 &&
                    tacticalSituation.enemyOffensiveApproachError?.let { it < Math.PI / 2 } == true
        }
    }
}
