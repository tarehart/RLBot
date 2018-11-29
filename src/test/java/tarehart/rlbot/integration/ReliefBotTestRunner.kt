package tarehart.rlbot.integration

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import org.junit.runners.Parameterized
import rlbot.manager.BotManager
import rlbot.pyinterop.PythonServer
import tarehart.rlbot.DEFAULT_PORT
import tarehart.rlbot.integration.ReliefBotTestRunner.Companion.lookupTestRunner
import tarehart.rlbot.integration.asserts.AssertStatus
import tarehart.rlbot.readPortFromFile
import java.lang.AssertionError
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.reflect.Method

// TODO: @Ignore is implemented, but @Before, @After, @Expected and @Rule are not implemented.
// TODO: Somewhere in the belly of com.intellij.junit4.JUnit4IdeaTestRunner, arguments are passed in
//       to filter which methods on each test case are called (So IntelliJ can run a subset of tests)
//       however these arguments are lost when JUnitCore is invoked. Likely instead of a Runner we
//       need to implement a higher level class that can take these arguments.
//       I can see IntelliJ would like to do some magic with parametrised tests but I can't work out
//       how to offer that to IntelliJ's runner from instead this runner.
// TODO: The bottom line is that you must run an entire test class, not such a subset of methods
class ReliefBotTestRunner(val test: Class<*>) : Runner() {

    private val testCases = arrayListOf<StateSettingTestCase>()

    /**
     * Setup the suite before running any tests. Such as running the bot serve
     */
    private fun setupSuite() {
        val port = readPortFromFile().orElse(DEFAULT_PORT)
        val pythonInterface = IntegrationPyInterface(BotManager())
        val pythonServer = PythonServer(pythonInterface, port)
        pythonServer.start()
    }

    override fun run(notifier: RunNotifier) {
        System.out.println("ReliefBot Test Runner is starting up test for: " + test.name)
        try {
            val testInstance = test.newInstance()
            setupSuite()
            for (method in test.methods.filter { it.isAnnotationPresent(Test::class.java) && !it.isAnnotationPresent(Ignore::class.java)}) {
                val description = Description.createTestDescription(test, method.name)
                try {
                    bindTestMethodToRunner(method, this)
                    notifier.fireTestStarted(description)
                    method.invoke(testInstance)
                    if (testCases.isEmpty()) {
                        throw RuntimeException("Test $test did not add any Test Cases!")
                    } else {
                        testCases.forEach { testCase ->
                            val harness = ReliefBotTestHarness(testCase)
                            harness.runTillComplete()

                            Assert.assertTrue(testCase.isComplete)
                            testCase.conditions.forEach {
                                if (it.status == AssertStatus.FAILED) {
                                    Assert.fail(it.message)
                                }
                            }
                        }
                    }
                    notifier.fireTestFinished(description)
                } catch (assertFail: AssertionError) {
                    notifier.fireTestFailure(Failure(description, assertFail))
                } finally {
                    cleanupRunnerBindings(this)
                }
            }
        } catch (ex: Exception) {
            if (ex is AssertionError) throw ex
            throw RuntimeException(ex)
        }
    }

    override fun getDescription(): Description {
        return Description.createTestDescription(test, "ReliefBot Test Runner: ${test.name}")
    }

    fun addTestCase(testCase: StateSettingTestCase) {
        testCases.add(testCase)
    }

    companion object {

        private val bindings: HashMap<Method, ReliefBotTestRunner> = HashMap()

        private fun bindTestMethodToRunner(method: Method, runner: ReliefBotTestRunner) {
            bindings.put(method, runner)
        }

        private fun cleanupRunnerBindings(runner: ReliefBotTestRunner) {
            bindings.keys.filter { bindings[it] == runner }.forEach {
                bindings.remove(it)
            }
        }

        fun lookupTestRunner(method: Method) : ReliefBotTestRunner? {
            if (method in bindings) {
                return bindings[method]
            }
            return null
        }
    }
}

// TODO: Perhaps we abstract away the useful parts of StateSettingTestCase in to an interface or abstract class
// TODO: Do we ever want a test case that doesn't state set?
fun assertReliefBotTestCase(testCase: StateSettingTestCase) {
    val trace = Thread.currentThread().stackTrace
    for (traceElement in trace) {
        try {
            val traceClass = Class.forName(traceElement.className)
            val traceMethod = traceClass.getMethod(traceElement.methodName)
            val testRunner = lookupTestRunner(traceMethod)
            if (testRunner != null) {
                testRunner.addTestCase(testCase)
                return
            }
        } catch (ex: Exception) {

        }
    }
    throw RuntimeException("Failed to find ReliefBotTestRunner instance for a TestCase: $testCase")
}