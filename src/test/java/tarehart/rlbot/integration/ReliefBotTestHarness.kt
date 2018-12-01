package tarehart.rlbot.integration

import rlbot.cppinterop.RLBotDll
import rlbot.manager.BotManager
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.bots.ReliefBot
import tarehart.rlbot.bots.Team

class ReliefBotTestHarness(private val testCase: StateSettingTestCase) {

    private val botManager = BotManager()

    private fun start() {

        var initialized = false
        println("Starting ReliefBot Test Harness.")

        while (!initialized) {
            try {
                RLBotDll.initialize("garbage path to make this thing fail fast until " +
                        "it gets initialized by another thread.")
                RLBotDll.setPlayerInputFlatbuffer(AgentOutput(), STANDARD_PLAYER_INDEX)
                testCase.setState()
                botManager.ensureStarted()
                initialized = true
                Thread.sleep(100) // Give the state a chance to take hold.
            } catch (e: Error) {
                e.printStackTrace()
                Thread.sleep(1000) // Wait a while to give python a chance to phone in.
            } catch (e: Exception) {
                e.printStackTrace()
                Thread.sleep(1000) // Wait a while to give python a chance to phone in.
            }
        }

        val reliefBot = ReliefBot(Team.BLUE, STANDARD_PLAYER_INDEX)
        botManager.ensureBotRegistered(STANDARD_PLAYER_INDEX) { reliefBot }
        reliefBot.registerBundleListener(testCase)
    }

    private fun stop() {
        botManager.retireBot(STANDARD_PLAYER_INDEX)
        botManager.shutDown()
        Thread.sleep(200) // This seems to give ReliefBot a chance to finish its final cycle
    }

    fun runTillComplete() {
        start()
        while(!testCase.isComplete && 0 in botManager.runningBotIndices) {
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