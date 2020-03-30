package tarehart.rlbot.integration.defense

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.PlaneBreakAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.steps.teamwork.RotateBackToGoalStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class RotateOutTest: StateSettingAbstractTest() {

    @Test
    fun testRotateOutWithBallOnRight() {
        val testCase = createTestCase(30F)
        runTestCase(testCase)
    }

    @Test
    fun testRotateOutWithBallOnLeft() {
        val testCase = createTestCase(-30F)
        runTestCase(testCase)
    }

    fun createTestCase(ballX: Float): StateSettingTestCase {

        return StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(ballX, 0F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, 0F, 0F))
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
                        timeLimit = Duration.ofSeconds(600000)).negate()),
                Plan(Posture.OVERRIDE).withStep(RotateBackToGoalStep())
                )
    }
}
