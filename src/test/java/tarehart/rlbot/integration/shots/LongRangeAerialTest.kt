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

class LongRangeAerialTest: StateSettingAbstractTest() {

    @Test
    fun testBallMovingSideways() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-20F, 60F, 10F))
                                .withVelocity(StateVector(10F, 0F, 25F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -30F, ManeuverMath.BASE_CAR_Z.toFloat()))
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

                        BallTouchAssert(Duration.ofSeconds(3.0))))

        runTestCase(testCase)
    }

    @Test
    fun testBallMovingSidewaysAndCarAtAngle() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-20F, 60F, 10F))
                                .withVelocity(StateVector(10F, 0F, 24F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(60F, 20F, ManeuverMath.BASE_CAR_Z.toFloat()))
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

                        BallTouchAssert(Duration.ofSeconds(4.0))))

        runTestCase(testCase)
    }

    @Test
    fun testBackboardHit() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 60F, 10F))
                                .withVelocity(StateVector(0F, 25F, 25F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(10F, -30F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 20F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(5.0),
                                delayWhenBallFloating = false),

                        BallTouchAssert(Duration.ofSeconds(4.0))))

        runTestCase(testCase)
    }

    @Test
    fun testVeryFar() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 60F, 10F))
                                .withVelocity(StateVector(0F, 5F, -50F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(100F).withPhysics(PhysicsState()
                                .withLocation(StateVector(10F, -80F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 20F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(7.0),
                                delayWhenBallFloating = false),

                        BallTouchAssert(Duration.ofSeconds(7.0))))

        runTestCase(testCase)
    }
}
