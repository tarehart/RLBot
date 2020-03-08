package tarehart.rlbot.integration

import rlbot.cppinterop.RLBotDll
import rlbot.manager.BotManager
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.bots.ReliefBot
import tarehart.rlbot.bots.TacticalBot
import tarehart.rlbot.bots.Team
import tarehart.rlbot.integration.asserts.AssertStatus
import tarehart.rlbot.integration.metrics.TimeMetric
import tarehart.rlbot.time.Duration
import java.lang.AssertionError

class ReliefBotTestHarness(private val testCase: StateSettingTestCase) {

    private val botManager = BotManager()
    private lateinit var botInstance: TacticalBot

    private fun start() {

        var initialized = false
        println("Starting ReliefBot Test Harness.")

        while (!initialized) {
            try {
                RLBotDll.initialize("garbage path to make this thing fail fast until " +
                        "it gets initialized by another thread.")
                RLBotDll.setPlayerInputFlatbuffer(AgentOutput(), STANDARD_PLAYER_INDEX)
                botManager.ensureStarted()
                initialized = true
                System.out.println("Setting state")
                testCase.setState()
                Thread.sleep(100) // Give the state a chance to take hold.
            } catch (e: Error) {
                e.printStackTrace()
                Thread.sleep(1000) // Wait a while to give python a chance to phone in.
            } catch (e: Exception) {
                e.printStackTrace()
                Thread.sleep(1000) // Wait a while to give python a chance to phone in.
            }
        }

        System.out.println("Booting up ReliefBot")
        botInstance = ReliefBot(Team.BLUE, STANDARD_PLAYER_INDEX)
        botInstance.currentPlan = testCase.initialPlan
        botManager.ensureBotRegistered(STANDARD_PLAYER_INDEX) { botInstance }

        while(!botInstance.loaded) {
            Thread.sleep(16)
        }

        botInstance.registerBundleListener(testCase)
    }

    private fun stop() {
        if ( ::botInstance.isInitialized ) {
            botInstance.retire()
        }
        botManager.retireBot(STANDARD_PLAYER_INDEX)
        botManager.shutDown()
        Thread.sleep(400) // This seems to give ReliefBot a chance to finish its final cycle
    }

    fun runTillComplete() {
        start()
        while(!testCase.isComplete && 0 in botManager.runningBotIndices) {
            Thread.sleep(100)
        }
        stop()
        // Spit out the metrics, would be nice to log these to a file so we can compare them over time.
        // TODO: Log metrics or post to a server
        // TODO: Log/Display the input values for the assertions so we can see them change over time
        testCase.conditions.filter { !it.metrics.isEmpty() }.forEach {
            val metNotMet = if (it.status == AssertStatus.SUCCEEDED) "met" else "NOT MET"
            println("Condition ${it.javaClass.simpleName} ${metNotMet} with metrics")
            it.metrics.forEach {
                println("\t${it.toString()}")
            }
        }
    }

    companion object {
        // The test cases will assume that index 0:
        // - is on the blue team
        // - is registered as rlbot or party_member_bot
        // - is not receiving inputs from any other process
        const val STANDARD_PLAYER_INDEX = 0
    }

}
