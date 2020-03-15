package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.tactics.TacticsAdvisor

class AdversityBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex) {
    override fun getNewTacticsAdvisor(input: AgentInput): TacticsAdvisor {
        return AdversityBotTacticsAdvisor(input)
    }
}
