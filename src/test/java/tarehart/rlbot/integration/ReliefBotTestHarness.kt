package tarehart.rlbot.integration

import rlbot.cppinterop.RLBotDll
import rlbot.manager.BotManager
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.bots.ReliefBot
import tarehart.rlbot.bots.Team

class ReliefBotTestHarness(private val testCase: StateSettingTestCase) {

    private val botManager = BotManager()

    private fun start() {

        botManager.ensureStarted()
        var initialized = false
        while (!initialized) {
            try {
                RLBotDll.setPlayerInputFlatbuffer(AgentOutput(), STANDARD_PLAYER_INDEX)
                testCase.setState()
                initialized = true
                Thread.sleep(100) // Give the state a chance to take hold.
            } catch (e: Error) {
                e.printStackTrace()
            }
        }

        val reliefBot = ReliefBot(Team.BLUE, STANDARD_PLAYER_INDEX)
        botManager.ensureBotRegistered(STANDARD_PLAYER_INDEX) { reliefBot }
        reliefBot.registerBundleListener(testCase)
    }

    private fun stop() {
        botManager.retireBot(STANDARD_PLAYER_INDEX)
        botManager.shutDown()
    }

    fun runTillComplete() {
        start()
        while(!testCase.isComplete) {
            Thread.sleep(100)
        }
        stop()
    }

    companion object {
        // The test cases will assume that index 0:
        // - is on the blue team
        // - is registered as rlbot or party_member_bot
        // - is not receiving inputs from any other process
        const val STANDARD_PLAYER_INDEX = 0
    }

}