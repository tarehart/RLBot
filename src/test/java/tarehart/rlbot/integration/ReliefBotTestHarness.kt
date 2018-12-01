package tarehart.rlbot.integration

import rlbot.cppinterop.RLBotDll
import rlbot.manager.BotManager
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.bots.ReliefBot
import tarehart.rlbot.bots.Team
import java.io.IOException

class ReliefBotTestHarness(private val testCase: StateSettingTestCase) {

    private val botManager = BotManager()

    private fun start() {

        var initialized = false
        var warningCounter = 50
        println("Starting ReliefBot Test Harness.")

        while (!initialized) {
            try {
                RLBotDll.setPlayerInputFlatbuffer(AgentOutput(), STANDARD_PLAYER_INDEX)
                testCase.setState()
                initialized = true
            } catch (e: java.lang.UnsatisfiedLinkError) {
                if (warningCounter-- == 0) {
                    println("Still waiting for the framework to be ready...")
                    warningCounter = 50
                }
            } catch (e: Error) {
                // Since we capsure the unsatisfied link separately
                // It may be worth just dying at this point.
                e.printStackTrace()
                Thread.sleep(900)
            } finally {
                Thread.sleep(100)
            }
        }

        warningCounter = 50
        println("Python Framework is ready, waiting for game to start...")
        while (true) {
            try {
                RLBotDll.getFieldInfo()
                break
            } catch (e: java.io.IOException) {
                if (warningCounter-- == 0) {
                    println("Still waiting for the game to be start...")
                    warningCounter = 50
                }
                Thread.sleep(100)
            }
        }

        botManager.ensureStarted()
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