package tarehart.rlbot.integration.shots

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import rlbot.gamestate.*
import rlbot.manager.BotManager
import rlbot.pyinterop.PythonServer
import tarehart.rlbot.DEFAULT_PORT
import tarehart.rlbot.integration.IntegrationPyInterface
import tarehart.rlbot.integration.ReliefBotTestHarness
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.AssertStatus
import tarehart.rlbot.integration.asserts.PlaneBreakAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.readPortFromFile
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class StationaryShotTest {

    @Test
    fun testStraightShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 10F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector.ZERO)
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -20F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(5.0))))

        val harness = ReliefBotTestHarness(testCase)
        harness.runTillComplete()

        Assert.assertTrue(testCase.isComplete)
        testCase.conditions.forEach {
            if (it.status == AssertStatus.FAILED)  {
                Assert.fail(it.message)
            }
        }
    }

    // This test case is currently failing for me. Something to work on soon!
    @Test
    fun testAngledShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 30F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector.ZERO)
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(30F, -20F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(5.0))))

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

        @BeforeClass
        @JvmStatic
        fun startListening() {
            val port = readPortFromFile().orElse(DEFAULT_PORT)
            val pythonInterface = IntegrationPyInterface(BotManager())
            val pythonServer = PythonServer(pythonInterface, port)
            pythonServer.start()
        }
    }

}