package tarehart.rlbot.hoops.tacticalstate

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.tactics.TacticalSituation
import tarehart.rlbot.time.Duration

class IdleState : TacticalState {

    override fun muse(input: AgentInput, situation: TacticalSituation): TacticalState {
        if (input.ballPosition.flatten().isZero) {
            return KickoffState()
        }
        return this
    }

    override fun urgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?) : Plan?{
        return null
    }

    override fun newPlan(input: AgentInput, situation: TacticalSituation) : Plan {
        // println("I don't know what to do, empty plan for me")
        return Plan()
    }
}