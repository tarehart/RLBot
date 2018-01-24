package tarehart.rlbot

import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.planning.TacticsAdvisor
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep

class DemolitionBot(team: Bot.Team, playerIndex: Int) : Bot(team, playerIndex) {

    override fun getOutput(input: AgentInput): AgentOutput {

        if (noActivePlanWithPosture(Plan.Posture.OVERRIDE)) {
            currentPlan = Plan(Plan.Posture.OVERRIDE).withStep(GetBoostStep()).withStep(DemolishEnemyStep())
        }

        return currentPlan?.getOutput(input) ?: AgentOutput()
    }
}
