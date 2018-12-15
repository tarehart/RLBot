package tarehart.rlbot.integration.defense

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class GoalEscapeTest: StateSettingAbstractTest() {

    @Test
    fun testEscapeAndClear() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(60F, -90F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(-10F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -115F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, 0F, 0F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(3.5))))

        runTestCase(testCase)
    }

    @Test
    fun testEscapeAndClearOtherSide() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-60F, -90F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(10F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -115F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat(), 0F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(3.5))))

        runTestCase(testCase)
    }

}