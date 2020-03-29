package tarehart.rlbot

import tarehart.rlbot.planning.TeamPlan
import tarehart.rlbot.planning.ZonePlan
import tarehart.rlbot.tactics.SoccerTacticsAdvisor
import tarehart.rlbot.tactics.SpikeRushTacticsAdvisor
import tarehart.rlbot.tactics.TacticalSituation

class TacticalBundle(val agentInput: AgentInput, val tacticalSituation: TacticalSituation, val teamPlan: TeamPlan? = null, val zonePlan: ZonePlan) {
    companion object {
        private var dummyTacticsAdvisor = SpikeRushTacticsAdvisor()

        fun dummy(input: AgentInput): TacticalBundle {
            return dummyTacticsAdvisor.assessSituation(input, null)
        }
    }
}
