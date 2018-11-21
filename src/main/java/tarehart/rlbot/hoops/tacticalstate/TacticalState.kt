package tarehart.rlbot.hoops.tacticalstate

import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.tactics.TacticalSituation

interface TacticalState {
    fun muse(bundle: TacticalBundle): TacticalState
    fun urgentPlan(bundle: TacticalBundle, currentPlan: Plan?) : Plan?
    fun newPlan(bundle: TacticalBundle) : Plan
}