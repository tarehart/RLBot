package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.TacticsAdvisor
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal

class ReliefBot(team: Team, playerIndex: Int) : Bot(team, playerIndex) {

    private val tacticsAdvisor: TacticsAdvisor = TacticsAdvisor()

    override fun getOutput(input: AgentInput): AgentOutput {

        val car = input.myCarData
        val ballPath = ArenaModel.predictBallPath(input)
        val situation = tacticsAdvisor.assessSituation(input, ballPath, currentPlan)

        tacticsAdvisor.findMoreUrgentPlan(input, situation, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = tacticsAdvisor.makeFreshPlan(input, situation)
            //currentPlan = Plan().withStep(FlexibleKickStep(KickAtEnemyGoal()))
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(input)?.let { return it }
            }
        }

        return SteerUtil.steerTowardGroundPosition(car, input.boostData, input.ballPosition.flatten()).withBoost(false)
    }
}
