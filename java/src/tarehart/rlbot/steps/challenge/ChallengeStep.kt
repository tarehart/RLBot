package tarehart.rlbot.steps.challenge

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.intercept.AirTouchPlanner
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.*
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.InterceptStep
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

    override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {
        val tacticalSituation = TacticsTelemetry.get(input.playerIndex) ?: return false
        return !threatExists(tacticalSituation)
    }

    private fun threatExists(tacticalSituation: TacticalSituation): Boolean {
        return tacticalSituation.ballAdvantage.seconds < 1.0 &&
                tacticalSituation.enemyOffensiveApproachError?.let { it < Math.PI / 3 } == true
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData

        if (originalTouch == null) {
            originalTouch = input.latestBallTouch.orElse(null)
        } else {

            if (originalTouch?.position ?: Vector3() != input.latestBallTouch.map({it.position}).orElse(Vector3())) {
                // There has been a new ball touch.
                println("Ball has been touched, quitting challenge", input.playerIndex)
                return null
            }
        }

        val tacticalSituation = TacticsTelemetry.get(input.playerIndex) ?: return null
        val ballAdvantage = tacticalSituation.ballAdvantage
        if (ballAdvantage.seconds > 2.0) {
            return null // We can probably go for a shot now.
        }

        val enemyContact = tacticalSituation.expectedEnemyContact ?: return null

        if (enemyContact.space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
            return null
        }

        val enemyShotLine = GoalUtil.getOwnGoal(input.team).center - enemyContact.space

        val flatPosition = car.position.flatten()
        val contactDistance = flatPosition.distance(enemyContact.space.flatten())
        val defensiveNode = enemyContact.space.flatten() + enemyShotLine.flatten().scaledToMagnitude(backoff)
        latestDefensiveNode = defensiveNode

        val defensiveNodeDistance = flatPosition.distance(defensiveNode)

        if (defensiveNodeDistance < backoff + 15) {
            startPlan(
                    Plan(Plan.Posture.DEFENSIVE)
                            .withStep(InterceptStep(enemyShotLine.scaledToMagnitude(1.5))),
                    input)
        }

        SteerUtil.getSensibleFlip(car, defensiveNode)?.let { return startPlan(it, input) }

        return SteerUtil.steerTowardGroundPosition(car, input.boostData, defensiveNode)
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
}
