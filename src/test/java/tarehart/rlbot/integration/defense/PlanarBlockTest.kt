package tarehart.rlbot.integration.defense

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.PlaneBreakAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class PlanarBlockTest: StateSettingAbstractTest() {

    @Test
    fun testForwardBlock() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(13F, -20F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -50F, 16.5F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -100F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, 0F, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                        plane = PlaneBreakAssert.OWN_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(4.2)).negate()))

        runTestCase(testCase)
    }

    @Test
    fun testBackwardBlock() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-13F, -20F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -50F, 16.5F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -100F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, 0F, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.OWN_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(4.2)).negate()))

        runTestCase(testCase)
    }

    @Test
    fun testForwardBottom() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(13F, -40F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -70F, 0))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -100F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, 0F, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.OWN_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(4.2)).negate()))

        runTestCase(testCase)
    }

    @Test
    fun testBackwardBottom() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-13F, -40F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -70F, 0))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -100F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, 0F, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.OWN_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(4.2)).negate()))

        runTestCase(testCase)
    }

    @Test
    fun testDeerInHeadlights() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-13F, -20F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -50F, 0))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -100F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() * .3F, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.OWN_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(4.2)).negate()))

        runTestCase(testCase)
    }

    @Test
    fun testSlowRoller() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-1F, -40F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -40F, 0))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -100F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat(), 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.OWN_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(4.2)).negate()))

        runTestCase(testCase)
    }
}
