package tarehart.rlbot.integration.shots

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.integration.asserts.PlaneBreakAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class LongRunupShotTest: StateSettingAbstractTest() {

    @Test
    fun testLongRunupBouncing() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-50F, 40F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(10F, 0F, 20.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -80F, ManeuverMath.BASE_CAR_Z))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(8.0),
                        delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(5))))

        runTestCase(testCase)
    }
}
