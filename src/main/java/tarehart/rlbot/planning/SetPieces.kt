package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.travel.LineUpInReverseStep
import tarehart.rlbot.time.Duration

object SetPieces {

    fun speedupFlip(): Plan {

        return Plan()
                .unstoppable()
                .withStep(BlindStep(Duration.ofSeconds(.15),
                        AgentOutput()
                                .withPitch(-0.3)
                                .withJump(true)
                                .withYaw(1.0)
                                .withThrottle(1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.025),
                        AgentOutput()
                                .withPitch(-1.0)
                                .withThrottle(1.0)
                ))
                .withStep(BlindStep(Duration.ofSeconds(.3),
                        AgentOutput()
                                .withJump(true)
                                .withThrottle(1.0)
                                .withYaw(-0.6)
                                .withPitch(-1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.5),
                        AgentOutput()
                                .withThrottle(1.0)
                                .withPitch(-1.0)
                ))
                .withStep(LandGracefullyStep { it.agentInput.myCarData.velocity.flatten()})
    }

    fun halfFlip(waypoint: Vector2): Plan {

        return Plan()
                .unstoppable()
                .withStep(LineUpInReverseStep(waypoint))
                .withStep(BlindStep(Duration.ofSeconds(.05),
                        AgentOutput()
                                .withPitch(1.0)
                                .withJump(true)
                                .withThrottle(-1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.05),
                        AgentOutput()
                                .withPitch(1.0)
                                .withThrottle(-1.0)
                ))
                .withStep(BlindStep(Duration.ofSeconds(.05),
                        AgentOutput()
                                .withJump(true)
                                .withPitch(1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.15),
                        AgentOutput()
                                .withPitch(1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.4),
                        AgentOutput()
                                .withBoost()
                                .withPitch(-1.0)
                ))
                .withStep(BlindStep(Duration.ofSeconds(.5),
                        AgentOutput()
                                .withBoost()
                                .withPitch(-1.0)
                                .withRoll(1.0)
                ))
    }

    fun jumpSuperHigh(howHigh: Double): Plan {
        return Plan()
                .withStep(BlindStep(Duration.ofSeconds(.3), AgentOutput()
                        .withJump(true)
                        .withPitch(1.0)
                ))
                .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput()
                        .withPitch(1.0)
                        .withBoost(true)
                ))
                .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput()
                        .withBoost(true)
                        .withJump(true)
                ))
                .withStep(BlindStep(Duration.ofSeconds(howHigh / 10), AgentOutput()
                        .withJump(true)
                        .withBoost(true)
                ))
                .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
    }
}
