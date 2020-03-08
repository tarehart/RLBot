package tarehart.rlbot.integration.shots

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.integration.asserts.PlaneBreakAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal
import tarehart.rlbot.steps.strikes.SlotKickStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

// 0: 0

class SlotKickTest: StateSettingAbstractTest() {

    @Test
    fun testStraightShot() {
        runTestCase(makeCase(0))
    }

    @Test
    fun testAngledShot() {
        runTestCase(makeCase(30))
    }

    fun makeCase(offset: Number): StateSettingTestCase {
        return StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 10F, ArenaModel.BALL_RADIUS + 1))
                                .withVelocity(StateVector(0, 0, 0.1))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(0F).withPhysics(PhysicsState()
                                .withLocation(StateVector(offset, -40F, ManeuverMath.BASE_CAR_Z))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(4.0),
                        delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(3.0))),
                Plan(Posture.OVERRIDE).withStep(SlotKickStep(KickAtEnemyGoal())))
    }
}
