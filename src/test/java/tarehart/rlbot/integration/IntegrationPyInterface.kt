package tarehart.rlbot.integration

import rlbot.Bot
import rlbot.cppinterop.RLBotDll
import rlbot.manager.BotManager
import rlbot.pyinterop.DefaultPythonInterface
import java.io.IOException

/**
 * The public methods of this class will be called directly from the python component of the RLBot framework.
 */
class IntegrationPyInterface(botManager: BotManager) : DefaultPythonInterface(botManager) {

    override fun initBot(index: Int, botType: String, team: Int): Bot {
        throw NotImplementedError()
    }

    override fun ensureBotRegistered(index: Int, botType: String, team: Int) {
        // Do nothing. We will be initializing bots ourselves.
    }

    override fun ensureStarted(interfaceDllPath: String) {
        try {
            RLBotDll.initialize(interfaceDllPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
