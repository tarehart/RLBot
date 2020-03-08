package tarehart.rlbot.integration.shots

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.integration.asserts.PlaneBreakAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal
import tarehart.rlbot.steps.strikes.SlotKickStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

// Ball change in velocity
// 0:   0
// 0.5: (4.85, 43.67, 13.24)
// 1:   (10.65, 41.58, 12.49)
// 2.5: (22.04, 17.74, 5.38)
// 3:   miss


class ShotAngleTester: StateSettingAbstractTest() {

    @Test
    fun testOffset() {
        runTestCase(makeCase(0))
    }

    fun makeCase(offset: Number): StateSettingTestCase {
        return StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0, 0, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0, 0, 0.1))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(offset, -30F, ManeuverMath.BASE_CAR_Z))
                                .withVelocity(StateVector(0F, 20F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(5.0),
                        delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(3.0))),
                Plan(Posture.OVERRIDE).withStep(BlindStep(Duration.ofSeconds(6), AgentOutput().withThrottle(1).withBoost())))
    }
}
