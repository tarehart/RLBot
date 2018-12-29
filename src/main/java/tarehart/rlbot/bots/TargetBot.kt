package tarehart.rlbot.bots

import rlbot.gamestate.*
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.hoops.HoopsZone
import tarehart.rlbot.intercept.strike.AerialStrike
import tarehart.rlbot.intercept.strike.SideHitStrike
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.state.ResetLoop
import tarehart.rlbot.steps.state.SetStateStep
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import kotlin.math.PI

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
                        .withLocation(StateVector(35F, -50F, 30F))
                        .withVelocity(StateVector(0F, 8F, 0F))
                        .withAngularVelocity(StateVector(0F, 0F, 0F))
                        .withRotation(DesiredRotation(randomRotation(), randomRotation(), randomRotation()))
                )) },
            Duration.ofSeconds(3.0))

    private fun randomSign() = if (Math.random() > 0.5) 1F else -1F
    private fun randomUniform(min: Float, max: Float) = (min + Math.random() * (max - min)).toFloat()
    private fun classicStateVector(x: Float, y: Float, z: Float) =
            StateVector(-x / Vector3.PACKET_DISTANCE_TO_CLASSIC.toFloat(), y / Vector3.PACKET_DISTANCE_TO_CLASSIC.toFloat(), z / Vector3.PACKET_DISTANCE_TO_CLASSIC.toFloat())

    private val aerialTestLoop = ResetLoop({
        GameState()
                .withBallState(BallState().withPhysics(
                        ({
                            bsign: Float -> PhysicsState()
                                .withLocation(classicStateVector(randomUniform(-3500F, -3000F) * bsign,
                                    randomUniform(-500F, 500F),
                                    randomUniform(150F, 500F)))
                                .withVelocity(classicStateVector(randomUniform(1000F, 1500F) * bsign,
                                        randomUniform(-500F, 500F),
                                        randomUniform(1200F, 1500F)))
                        })(randomSign())
                ))
                .withCarState(0, CarState().withPhysics(
                        ({ csign: Float ->
                            PhysicsState()
                                    .withLocation(classicStateVector(randomUniform(-1000f, 1000f), randomUniform(-4500f, -4000f) * csign, 25f))
                                    .withVelocity(classicStateVector(0F, 1000F * csign, 0F))
                                    .withAngularVelocity(classicStateVector(0F, 0F, 0F))
                                    .withRotation(DesiredRotation(0F, 0.5F * Math.PI.toFloat() * csign /* * csign */, 0F))
                        })(randomSign())
                ))
                },
            Duration.ofSeconds(5.0))

    private val hoopKickoffLoop = ResetLoop({
        GameState()
                .withBallState(BallState().withPhysics(PhysicsState()
                         .withLocation(StateVector(0f, 0f, 1.96779998f ))
                         .withVelocity(StateVector(0f, 0f, 0f))
                         .withAngularVelocity(StateVector(0f, 0f, 0f))
                         .withRotation(DesiredRotation(0f, 0f, 0f))))

                .withCarState(0, CarState().withPhysics(
                        ({ zone : HoopsZone ->
                            PhysicsState()
                                    .withLocation(StateVector(zone.center.x.toFloat(), zone.center.y.toFloat(), zone.center.z.toFloat()))
                                    .withRotation(DesiredRotation(0F, (0.5F * Math.PI.toFloat() * if (zone.isBlueTeam) 1 else -1 ), 0F))
                                    .withVelocity(StateVector(0F, 0F, 0F))
                                    .withAngularVelocity(StateVector(0F, 0F, 0F))
                        })( HoopsZone.KICKOFF_BLUE_FORWARD_LEFT )
                ).withBoostAmount(33.0F))

                // Team member
                .withCarState(1, CarState().withPhysics(
                        ({ zone : HoopsZone ->
                            PhysicsState()
                                    .withLocation(StateVector(zone.center.x.toFloat(), zone.center.y.toFloat(), zone.center.z.toFloat()))
                                    .withRotation(DesiredRotation(0F, (0.5F * Math.PI.toFloat() * if (zone.isBlueTeam) 1 else -1 ), 0F))
                                    .withVelocity(StateVector(0F, 0F, 0F))
                                    .withAngularVelocity(StateVector(0F, 0F, 0F))
                        })( HoopsZone.KICKOFF_BLUE_FORWARD_RIGHT )
                ).withBoostAmount(33.0F))

                .withCarState(2, CarState().withPhysics(
                        PhysicsState()
                                .withLocation(StateVector(-50F, 0F, 0F))
                ))
    }, Duration.ofSeconds(3.5))

    private val carryLoop = ResetLoop({
        GameState()
                .withBallState(BallState().withPhysics(PhysicsState()
                        .withLocation(StateVector(50f, -90.013f, 2.9f))
                        .withVelocity(StateVector(0f, 5f, 0f))
                        .withAngularVelocity(StateVector(0f, 0f, 0f))
                        .withRotation(DesiredRotation(0f, 0f, 0f))))

                .withCarState(0, CarState()
                        .withPhysics(PhysicsState()
                            .withLocation(StateVector(50f, -90f, 0.34f))
                            .withVelocity(StateVector(0f, 5f, 0f))
                            .withAngularVelocity(DesiredVector3(0f, 0f, 0f))
                            .withRotation(DesiredRotation(0f, (PI / 2).toFloat(), 0f)))
                        .withBoostAmount(33.0F))

    }, Duration.ofSeconds(30.0))

    override fun getOutput(input: AgentInput): AgentOutput {
        return runCarryBotTest(input)
    }

    private fun runCarryBotTest(input: AgentInput): AgentOutput {
        val carryBot = input.allCars[1 - input.playerIndex]
        val distance = input.ballPosition.flatten().distance(carryBot.position.flatten())
        if (carryLoop.getDurationSinceReset(input.time).seconds > 1 && distance > 3) {
            carryLoop.reset(input.time)
        } else {
            carryLoop.check(input.time)
        }
        return AgentOutput()
    }

    private fun runHoopsKickoffTest(input: AgentInput): AgentOutput {

        if (hoopKickoffLoop.check(input.time)) {
            currentPlan = Plan()
                    .withStep(BlindStep(Duration.ofMillis(1000), AgentOutput()))
                    .withStep(SetStateStep(GameState().withBallState(BallState().withPhysics(PhysicsState().withVelocity(StateVector(0F, 0F, 20F))))))

        }
        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(TacticalBundle.dummy(input))?.let { return it }
            }
        }
        return AgentOutput()
    }

    var aerialTestCounter = 0
    var aerialSuccessCounter = 0
    var aerialCollisionThisTest = false
    var ballWasValidated = false

    // Aerial integration test, can ReliefBot smash the ball in the air, every time.
    // Follow up test would be, can ReliefBot hit the ball at a target, i.e a point on goal or at a wall.
    // But that can be another test for a later time.
    private fun runAerialTest(input: AgentInput): AgentOutput {
        val car = input.myCarData

        if (input.playerIndex == 0) {
            val timeSinceLastTouch = input.time.toMillis() - (input.latestBallTouch?.time?.toMillis() ?: 0)
            // Check if we have hit the ball
            if (!aerialCollisionThisTest && input.latestBallTouch?.playerIndex == input.playerIndex &&
                    timeSinceLastTouch < 100) {
                aerialCollisionThisTest = true
                if (ballWasValidated) {
                    if (input.ballPosition.z > 10F) {
                        aerialSuccessCounter++
                    }
                    aerialTestCounter++
                }
                ballWasValidated = false
                aerialTestLoop.reset(input.time) // Reset early because we hit the ball, good job
                print("Touch Success: ")
                print(aerialSuccessCounter)
                print("/")
                println(aerialTestCounter)
            } else if (timeSinceLastTouch > 200) {
                aerialCollisionThisTest = false
            }

            if (!ballWasValidated) { // If the ball doesn't reach a certain height I wont count this test
                if (input.ballPosition.z > 10F) {
                    println("Ball validated")
                    ballWasValidated = true
                }
            }

            if (aerialTestLoop.check(input.time)) {
                if (ballWasValidated) aerialTestCounter ++
                aerialCollisionThisTest = false
                print("Touch Success: ")
                print(aerialSuccessCounter)
                print("/")
                println(aerialTestCounter)
            }

            if (Plan.activePlanKt(currentPlan) == null) {
                currentPlan = AerialStrike.performDoubleJumpAerial(0.3, null)
            }

            currentPlan?.let {
                if (it.isComplete()) {
                    currentPlan = null
                } else {
                    it.getOutput(TacticalBundle.dummy(input))?.let { return it }
                }
            }
        }

        return AgentOutput()
    }

    private fun runOrientationTest(input: AgentInput): AgentOutput {
        val car = input.myCarData

        orientationTestLoop.check(input.time)

        /* RLBotDll.setGameState(
                GameState().withCarState(0, CarState().withPhysics(
                        PhysicsState()
                                .withVelocity(StateVector(0f, 0f, 0f))
                                .withLocation(StateVector(35F, -50F, 30F))
                )).buildPacket()
        ) */

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = Plan().withStep(LandGracefullyStep(facingFn = LandGracefullyStep.FACE_MOTION) )
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(TacticalBundle.dummy(input))?.let { return it }
            }
        }

        return AgentOutput()
    }

    private fun runDemolitionTest(input: AgentInput): AgentOutput {
        if (!input.matchInfo.roundActive) {
            return AgentOutput()
        }

        demolishTestLoop.check(input.time)

        if (Plan.activePlanKt(currentPlan) == null) {
            val enemy = input.getTeamRoster(input.team.opposite())[0]
            val distance = input.myCarData.position.distance(enemy.position)

            if (distance < 50) {
               currentPlan = SideHitStrike.jumpSideFlip(false, Duration.ofSeconds(0.2), false)
                //currentPlan = SetPieces.frontFlip()
//                currentPlan = Plan().unstoppable()
//                        .withStep(BlindStep(Duration.ofMillis(200), AgentOutput().withJump()))
//                        .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()))
//                        .withStep(BlindStep(Duration.ofMillis(500), AgentOutput().withJump()))
//                        .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
            }

            return AgentOutput().withThrottle(1.0).withBoost()
        }

        return currentPlan?.getOutput(TacticalBundle.dummy(input)) ?: AgentOutput()
    }
}

