package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep

class AdversityBot(team: Team, playerIndex: Int) : Bot(team, playerIndex) {

    override fun getOutput(input: AgentInput): AgentOutput {

        if (Plan.Posture.OVERRIDE.canInterrupt(currentPlan)) {
            currentPlan = FirstViableStepPlan(Plan.Posture.OVERRIDE)
                    .withStep(DemolishEnemyStep())
                    .withStep(GetBoostStep())
        }

        return currentPlan?.getOutput(input) ?: AgentOutput()
    }
}
