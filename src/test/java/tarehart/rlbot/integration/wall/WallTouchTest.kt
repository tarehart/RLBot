package tarehart.rlbot.integration.wall

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
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

// 0: 0

class WallTouchTest: StateSettingAbstractTest() {

    @Test
    fun wallTouchTest() {
        runTestCase(setupWallTouchScenario())
    }

    @Test
    fun defensiveWallTouchTest() {
        runTestCase(setupWallTouchDefensiveScenario())
    }

    @Test
    fun offTheWallTouchTest() {
        runTestCase(setupOffTheWallTouchScenario())
    }

    fun setupOffTheWallTouchScenario(): StateSettingTestCase {

        return StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS - 2, 0F, ArenaModel.BALL_RADIUS + 0.2))
                                .withVelocity(StateVector(0, 10, 25))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(40F, -20F, ManeuverMath.BASE_CAR_Z))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(7.0),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(3.0))),
                Plan(Posture.OVERRIDE).withStep(WallTouchStep()))
    }


    fun setupWallTouchScenario(): StateSettingTestCase {

        return StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-70, 0F, ArenaModel.BALL_RADIUS + 0.2))
                                .withVelocity(StateVector(-40, 10, 0.1))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(-40F, -20F, ManeuverMath.BASE_CAR_Z))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(7.0),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(3.0))),
                Plan(Posture.OVERRIDE).withStep(WallTouchStep()))
    }

    fun setupWallTouchDefensiveScenario(): StateSettingTestCase {

        return StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-40, -80F, ArenaModel.BALL_RADIUS + 0.2))
                                .withVelocity(StateVector(5, -50, 0.1))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(-40F, -60F, ManeuverMath.BASE_CAR_Z))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, -Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(7.0),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(3.0))),
                Plan(Posture.OVERRIDE).withStep(WallTouchStep()))
    }
}
