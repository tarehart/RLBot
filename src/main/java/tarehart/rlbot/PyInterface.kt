package tarehart.rlbot

import rlbot.Bot
import rlbot.manager.BotManager
import rlbot.py.PythonInterface
import tarehart.rlbot.bots.AdversityBot
import tarehart.rlbot.bots.AirBudBot
import tarehart.rlbot.bots.JumpingBeanBot
import tarehart.rlbot.bots.ReliefBot
import tarehart.rlbot.ui.StatusSummary

/**
 * The public methods of this class will be called directly from the python component of the RLBot framework.
 */
class PyInterface(private val botManager: BotManager, private val statusSummary: StatusSummary) : PythonInterface {


    fun ensureStarted() {
        botManager.ensureStarted()
    }

    fun shutdown() {
        botManager.shutDown()
    }

    fun ensureBotRegistered(index: Int, botType: String, team: Int) {
        botManager.ensureBotRegistered(index, { initBot(index, botType, team) })
    }

    fun retireBot(index: Int) {
        botManager.retireBot(index)
    }

    fun initBot(index: Int, botType: String, team: Int): Bot {
        val newBot: tarehart.rlbot.bots.BaseBot
        val teamEnum = AgentInput.teamFromInt(team)

        if (botType.startsWith("JumpingBean")) {
            newBot = JumpingBeanBot(teamEnum, index)
        } else if (botType.startsWith("AdversityBot")) {
            newBot = AdversityBot(teamEnum, index)
        } else if (botType.startsWith("Air Bud")) {
            newBot = AirBudBot(teamEnum, index)
        } else {
            newBot = ReliefBot(teamEnum, index)
        }
        statusSummary.markTeamRunning(teamEnum, index, newBot.debugWindow)

        return newBot
    }
}
