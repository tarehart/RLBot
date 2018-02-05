package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep

class AdversityBot(team: Team, playerIndex: Int) : Bot(team, playerIndex) {

    override fun getOutput(input: AgentInput): AgentOutput {

        if (Plan.Posture.OVERRIDE.canInterrupt(currentPlan)) {
            currentPlan = Plan(Plan.Posture.OVERRIDE).withStep(GetBoostStep()).withStep(DemolishEnemyStep())
        }

        return currentPlan?.getOutput(input) ?: AgentOutput()
    }
}
