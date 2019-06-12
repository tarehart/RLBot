package tarehart.rlbot

import rlbot.Bot
import rlbot.manager.BotManager
import rlbot.pyinterop.SocketServer
import tarehart.rlbot.bots.*
import tarehart.rlbot.ui.StatusSummary

/**
 * The public methods of this class will be called directly from the python component of the RLBot framework.
 */
class PyInterface(port: Int, botManager: BotManager, private val statusSummary: StatusSummary) :
        SocketServer(port, botManager) {

    override fun initBot(index: Int, botType: String, team: Int): Bot {
        val newBot: tarehart.rlbot.bots.BaseBot
        val teamEnum = AgentInput.teamFromInt(team)

        if (botType.startsWith("JumpingBean")) {
            newBot = JumpingBeanBot(teamEnum, index)
        } else if (botType.startsWith("AdversityBot")) {
            newBot = AdversityBot(teamEnum, index)
        } else if (botType.startsWith("Air Bud")) {
            newBot = AirBudBot(teamEnum, index)
        } else if (botType.startsWith("TargetBot")) {
            newBot = TargetBot(teamEnum, index)
        } else if (botType.startsWith("CarryBot")) {
            newBot = CarryBot(teamEnum, index)
        } else {
            newBot = ReliefBot(teamEnum, index)
        }
        statusSummary.markTeamRunning(teamEnum, index, newBot.debugWindow, newBot.detailFlagsPanel)

        return newBot
    }

    override fun retireBot(index: Int) {
        super.retireBot(index)

        statusSummary.removeBot(index)
    }
}
