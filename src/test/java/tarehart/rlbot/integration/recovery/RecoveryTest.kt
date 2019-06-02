package tarehart.rlbot.integration.recovery

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class RecoveryTest: StateSettingAbstractTest() {

    @Test
    fun testFlingAtWall() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(5F, 0F, 30F))
                                .withVelocity(StateVector(30F, 0F, 10F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(1F, 1F, 0F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(3.5))))

        runTestCase(testCase)
    }

    @Test
    fun testDropToFloor() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withCarState(0, CarState().withBoostAmount(45F).withPhysics(PhysicsState()
                                .withLocation(StateVector(5F, 0F, 30F))
                                .withRotation(DesiredRotation(0F, 0F, 1F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(3.5))))

        runTestCase(testCase)
    }

    @Test
    fun testDropToFloorRoll() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(5F, 70F, 10F))
                                .withRotation(DesiredRotation(0F, 0F, 1F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(1.5))))

        runTestCase(testCase)
    }

    @Test
    fun testDropToFloorPitch() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(5F, 70F, 10F))
                                .withRotation(DesiredRotation(1F, 0F, 0F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(1.5))))

        runTestCase(testCase)
    }

    @Test
    fun testDropToFloorCounterPitch() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(5F, 70F, 10F))
                                .withAngularVelocity(StateVector(0F, 1F, 0F))
                                .withRotation(DesiredRotation(2F, 0F, 0F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(1.5))))

        runTestCase(testCase)
    }

    @Test
    fun testMatchVelocity() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(5F, 70F, 10F))
                                .withAngularVelocity(StateVector(0F, 0F, 0.01F))
                                .withVelocity(StateVector(10F, 0F, 1F))
                        )),
                hashSetOf(BallTouchAssert(Duration.ofSeconds(1.5))))

        runTestCase(testCase)
    }
}