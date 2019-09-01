package tarehart.rlbot.integration.Defends

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

class ChaseBallSaves: StateSettingAbstractTest() {

    @Test
    fun testStraightDefend() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -15F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -30F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 10F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, -Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                        plane = PlaneBreakAssert.OWN_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(6.0)).negate()))

        runTestCase(testCase)
    }
}
