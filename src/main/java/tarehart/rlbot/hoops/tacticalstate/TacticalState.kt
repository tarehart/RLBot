package tarehart.rlbot.hoops.tacticalstate

import tarehart.rlbot.AgentInput
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.tactics.TacticalSituation

interface TacticalState {
    fun muse(bundle: TacticalBundle, situation: TacticalSituation): TacticalState
    fun urgentPlan(bundle: TacticalBundle, situation: TacticalSituation, currentPlan: Plan?) : Plan?
    fun newPlan(bundle: TacticalBundle, situation: TacticalSituation) : Plan
}