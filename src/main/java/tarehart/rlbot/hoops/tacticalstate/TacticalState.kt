package tarehart.rlbot.hoops.tacticalstate

import tarehart.rlbot.AgentInput
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.tactics.TacticalSituation

interface TacticalState {
    fun muse(input: AgentInput, situation: TacticalSituation): TacticalState
    fun urgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?) : Plan?
    fun newPlan(input: AgentInput, situation: TacticalSituation) : Plan
}