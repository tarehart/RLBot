package tarehart.rlbot

import tarehart.rlbot.planning.TeamPlan
import tarehart.rlbot.planning.ZonePlan
import tarehart.rlbot.tactics.TacticalSituation

class TacticalBundle(val agentInput: AgentInput, val tacticalSituation: TacticalSituation, val teamPlan: TeamPlan? = null, val zonePlan: ZonePlan) {
    companion object {
        fun dummy(input: AgentInput): TacticalBundle {
            return TacticalBundle(input, TacticalSituation.DUMMY, teamPlan = null, zonePlan = ZonePlan(input))
        }
    }
}