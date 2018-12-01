package tarehart.rlbot.integration

import org.junit.Assert
import org.junit.BeforeClass
import rlbot.manager.BotManager
import rlbot.pyinterop.PythonServer
import tarehart.rlbot.DEFAULT_PORT
import tarehart.rlbot.integration.asserts.AssertStatus
import tarehart.rlbot.readPortFromFile

abstract class StateSettingAbstractTest {

    protected fun runTestCase(testCase: StateSettingTestCase) {
        val harness = ReliefBotTestHarness(testCase)
        harness.runTillComplete()

        Assert.assertTrue(testCase.isComplete)
        testCase.conditions.forEach {
            if (it.status == AssertStatus.FAILED)  {
                Assert.fail(it.message)
            }
        }
    }

    companion object {
        var isListening = false

        @BeforeClass
        @JvmStatic
        fun startListening() {
            if (!isListening) {
                val port = readPortFromFile().orElse(DEFAULT_PORT)
                val pythonInterface = IntegrationPyInterface(BotManager())
                val pythonServer = PythonServer(pythonInterface, port)
                pythonServer.start()
                isListening = true
            }
        }
    }
}