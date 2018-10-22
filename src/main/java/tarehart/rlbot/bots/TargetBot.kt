package tarehart.rlbot.bots

import rlbot.gamestate.*
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.state.ResetLoop
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration

class TargetBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private val demolishTestLoop = ResetLoop({
            GameState()
                    .withBallState(BallState().withPhysics(PhysicsState().withLocation(StateVector(5000F, null, null))))
                    .withCarState(0, CarState().withPhysics(PhysicsState()
                            .withLocation(StateVector(0F, -50F, 0F))
                            .withVelocity(StateVector(0F, 10F, 0F))
                            .withAngularVelocity(StateVector(0F, 0F, 0F))
                            .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                    ))
                    .withCarState(1, CarState().withPhysics(PhysicsState()
                            .withLocation(StateVector(30F, 40F, 0F))
                            .withVelocity(StateVector(0F, 0F, 0F))
                            .withAngularVelocity(StateVector(0F, 0F, 0F))
                            .withRotation(DesiredRotation(0F, -Math.PI.toFloat() / 2, 0F))
                    )) },
            Duration.ofSeconds(4.0))

    private fun randomRotation() = ((Math.random() - 0.5) * Math.PI * 2).toFloat()

    private val orientationTestLoop = ResetLoop({
        GameState()
                .withCarState(0, CarState().withPhysics(PhysicsState()
                        .withLocation(StateVector(0F, -50F, 30F))
                        .withVelocity(StateVector(0F, 0F, 0F))
                        .withAngularVelocity(StateVector(0F, 0F, 0F))
                        .withRotation(DesiredRotation(randomRotation(), randomRotation(), randomRotation()))
                )) },
            Duration.ofSeconds(4.0))

    override fun getOutput(input: AgentInput): AgentOutput {
        return runOrientationTest(input)
    }

    private fun runOrientationTest(input: AgentInput): AgentOutput {
        val car = input.myCarData

        orientationTestLoop.check(input)

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = Plan().withStep(LandGracefullyStep { Vector2(0.0, 1.0) })
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(input)?.let { return it }
            }
        }

        return AgentOutput()
    }

    private fun runDemolitionTest(input: AgentInput): AgentOutput {
        if (!input.matchInfo.roundActive) {
            return AgentOutput()
        }

        demolishTestLoop.check(input)

        if (Plan.activePlanKt(currentPlan) == null) {
            val enemy = input.getTeamRoster(input.team.opposite())[0]
            val distance = input.myCarData.position.distance(enemy.position)

            if (distance < 50) {
               currentPlan = SetPieces.jumpSideFlip(false, Duration.ofSeconds(0.2), false)
                //currentPlan = SetPieces.frontFlip()
//                currentPlan = Plan().unstoppable()
//                        .withStep(BlindStep(Duration.ofMillis(200), AgentOutput().withJump()))
//                        .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()))
//                        .withStep(BlindStep(Duration.ofMillis(500), AgentOutput().withJump()))
//                        .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
            }

            return AgentOutput().withThrottle(1.0).withBoost()
        }

        return currentPlan?.getOutput(input) ?: AgentOutput()
    }
}

