package tarehart.rlbot

import rlbot.Bot
import rlbot.manager.BotManager
import rlbot.pyinterop.SocketServer
import tarehart.rlbot.bots.*

/**
 * The public methods of this class will be called directly from the python component of the RLBot framework.
 */
class PyInterface(port: Int, botManager: BotManager, private val bots: MutableMap<Int, BaseBot>) :
        SocketServer(port, botManager) {

    override fun initBot(index: Int, botType: String, team: Int): Bot {
        val newBot: BaseBot
        val teamEnum = AgentInput.teamFromInt(team)

        if (botType.startsWith("AdversityBot")) {
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

        bots[index] = newBot

        return newBot
    }

//    override fun retireBot(index: Int) {
//        BotHouse.retireBot(index)
//        // Normally this would check to see if any bots remain, and if not, shut down the process.
//        // However, I want to keep it hot and ready for PokebotTrainer.
//    }
}
