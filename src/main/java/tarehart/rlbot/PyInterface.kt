package tarehart.rlbot

import org.rlbot.twitch.action.server.api.handler.ActionEntity
import org.rlbot.twitch.action.server.api.handler.StandardActionHandler
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

        if (newBot is ActionEntity) {
            StandardActionHandler.registerActionEntity(botType, newBot)
        }

        return newBot
    }
}
