package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.steps.TapStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.landing.LandMindlesslyStep
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.steps.travel.LineUpInReverseStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

object SetPieces {

    fun frontFlip(): Plan {

        return Plan()
                .unstoppable()
                .withStep(BlindStep(Duration.ofSeconds(.05),
                        AgentOutput()
                                .withPitch(-1.0)
                                .withJump(true)
                                .withAcceleration(1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.05),
                        AgentOutput()
                                .withPitch(-1.0)
                                .withAcceleration(1.0)
                ))
                .withStep(BlindStep(Duration.ofSeconds(.3),
                        AgentOutput()
                                .withJump(true)
                                .withAcceleration(1.0)
                                .withPitch(-1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.5),
                        AgentOutput()
                                .withAcceleration(1.0)
                                .withPitch(-1.0)
                ))
                .withStep(LandGracefullyStep())
    }

    fun halfFlip(waypoint: Vector2): Plan {

        return Plan()
                .unstoppable()
                .withStep(LineUpInReverseStep(waypoint))
                .withStep(BlindStep(Duration.ofSeconds(.05),
                        AgentOutput()
                                .withPitch(1.0)
                                .withJump(true)
                                .withAcceleration(-1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.05),
                        AgentOutput()
                                .withPitch(1.0)
                                .withAcceleration(-1.0)
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
                                .withSteer(1.0)
                                .withSlide()
                ))
    }

    fun performAerial(tiltBackSeconds: Double): Plan {
        val tiltBackDuration = Duration.ofSeconds(tiltBackSeconds)

        return Plan()
                .withStep(BlindStep(tiltBackDuration, AgentOutput().withJump(true).withPitch(1.0)))
                .withStep(MidairStrikeStep(tiltBackDuration))
                .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
    }

    fun performDoubleJumpAerial(tiltBackSeconds: Double): Plan {
        val tiltBackDuration = Duration.ofSeconds(tiltBackSeconds)

        return Plan()
                .withStep(BlindStep(tiltBackDuration, AgentOutput().withJump(true).withPitch(1.0)))
                .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput()
                        .withPitch(1.0)
                        .withBoost(true)
                ))
                .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput()
                        .withBoost(true)
                        .withJump(true)
                ))
                .withStep(MidairStrikeStep(tiltBackDuration))
                .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
    }

    fun performJumpHit(jumpSeconds: Double): Plan {

        val pitchBackPortion = Math.min(.36, jumpSeconds)
        val driftUpPortion = jumpSeconds - pitchBackPortion

        val plan = Plan()
                .unstoppable()
                .withStep(BlindStep(Duration.ofSeconds(pitchBackPortion), AgentOutput()
                        .withJump(true)
                        .withPitch(1.0)
                ))

        if (driftUpPortion > 0) {
            plan.withStep(BlindStep(Duration.ofSeconds(driftUpPortion), AgentOutput().withJump(true)))
        }

        return plan
                .withStep(TapStep(1, AgentOutput()
                        .withPitch(-1.0)
                        .withJump(false)
                        .withAcceleration(1.0)))
                .withStep(TapStep(5, AgentOutput()
                        .withPitch(-1.0)
                        .withJump(true)
                        .withAcceleration(1.0)))
                .withStep(BlindStep(Duration.ofMillis(800), AgentOutput()
                        .withAcceleration(1.0)
                        .withPitch(-1.0)
                ))
                .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
    }

    fun sideFlip(flipLeft: Boolean): Plan {
        return Plan()
                .unstoppable()
                .withStep(TapStep(2,
                        AgentOutput()
                                .withJump(true)
                                .withAcceleration(1.0)))
                .withStep(TapStep(2,
                        AgentOutput()
                                .withAcceleration(1.0)
                ))
                .withStep(TapStep(2,
                        AgentOutput()
                                .withJump(true)
                                .withAcceleration(1.0)
                                .withSteer((if (flipLeft) -1 else 1).toDouble())))
                .withStep(LandMindlesslyStep())
    }

    fun jumpSideFlip(flipLeft: Boolean, rawJumpTime: Duration, hurry: Boolean): Plan {

        val jumpTime = if (rawJumpTime.millis < 0) Duration.ofMillis(0) else rawJumpTime

        return Plan()
                .unstoppable()
                .withStep(TapStep(2, AgentOutput()
                        .withJump(true)
                        .withAcceleration((if (hurry) 1 else 0).toDouble())))
                .withStep(BlindStep(jumpTime, AgentOutput()
                        .withJump(true)
                        .withBoost(hurry)
                        .withAcceleration(1.0)))
                .withStep(TapStep(2, AgentOutput()
                        .withAcceleration(1.0)
                ))
                .withStep(TapStep(2, AgentOutput()
                        .withJump(true)
                        .withAcceleration(1.0)
                        .withSteer((if (flipLeft) -1 else 1).toDouble())))
                .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
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
