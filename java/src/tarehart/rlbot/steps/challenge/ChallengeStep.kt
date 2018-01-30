package tarehart.rlbot.steps.challenge

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.intercept.AirTouchPlanner
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.*
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.tuning.BotLog.println

class ChallengeStep: NestedPlanStep() {

    private var originalTouch: BallTouch? = null

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
        val defensiveNode = enemyContact.space.flatten() + enemyShotLine.flatten().scaledToMagnitude(Math.min(15.0, contactDistance / 2))

        val defensiveNodeDistance = flatPosition.distance(defensiveNode)

        if (defensiveNodeDistance < 15) {
            startPlan(
                    Plan(Plan.Posture.DEFENSIVE)
                            .withStep(InterceptStep(enemyShotLine.scaledToMagnitude(1.5))),
                    input)
        }

        SteerUtil.getSensibleFlip(car, defensiveNode)?.let { return startPlan(it, input) }

        return SteerUtil.steerTowardGroundPosition(car, input.boostData, defensiveNode)
    }
}
