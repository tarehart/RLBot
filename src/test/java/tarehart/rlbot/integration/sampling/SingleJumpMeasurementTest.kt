package tarehart.rlbot.integration.sampling

import org.junit.Test
import rlbot.gamestate.CarState
import rlbot.gamestate.DesiredRotation
import rlbot.gamestate.GameState
import rlbot.gamestate.PhysicsState
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.integration.asserts.CarPrinterAssert
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class SingleJumpMeasurementTest: StateSettingAbstractTest() {
    @Test
    fun singleJump() {

        val blindSequence = BlindSequence()
                .withStep(BlindStep(Duration.ofSeconds(5), AgentOutput()
                        .withJump(true)
                ))


        val testCase = StateSettingTestCase(
                GameState()
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(5F, 0F, ManeuverMath.BASE_CAR_Z))
                                .withVelocity(StateVector(0, 0, 0))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, 0F, 0F))
                        )),
                conditions = hashSetOf(CarPrinterAssert(Duration.ofSeconds(5))),
                initialPlan = Plan()
                        .unstoppable()
                        .withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
                        .withStep(blindSequence))

        runTestCase(testCase)
    }
}
