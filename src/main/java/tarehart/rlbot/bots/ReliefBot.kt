package tarehart.rlbot.bots

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle

class ReliefBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex) {

    override fun getOutput(bundle: TacticalBundle): AgentOutput {
        return super.getOutput(bundle)
//        return AgentOutput()
    }
}
