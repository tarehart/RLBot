package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.LaunchChecklist
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog

class FlipHitStrike: StrikeProfile() {

    override val preDodgeTime = Duration.ZERO
    override val postDodgeTime = Duration.ofMillis(400)
    override val speedBoost = 10.0
    override val style = Style.FLIP_HIT

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkFlipHitReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing FlipHit!", car.playerIndex)
            return frontFlip()
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return isVerticallyAccessible(intercept.space.z)
    }

    private fun checkFlipHitReadiness(car: CarData, intercept: SpaceTime): LaunchChecklist {
        val checklist = LaunchChecklist()
        StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
        checklist.timeForIgnition = Duration.between(car.time + StrikePlanner.LATENCY_DURATION, intercept.time) <= strikeDuration
        return checklist
    }

    companion object {

        private const val MAX_HEIGHT_OF_BALL_FOR_FLIP_HIT = 3.2

        fun isVerticallyAccessible(height: Double): Boolean {
            return height <= MAX_HEIGHT_OF_BALL_FOR_FLIP_HIT
        }

        fun frontFlip(): Plan {

            return Plan()
                    .unstoppable()
                    .withStep(BlindStep(Duration.ofSeconds(.15),
                            AgentOutput()
                                    .withPitch(-0.3)
                                    .withJump(true)
                                    .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofSeconds(.02),
                            AgentOutput()
                                    .withPitch(-1.0)
                                    .withThrottle(1.0)
                    ))
                    .withStep(BlindStep(Duration.ofSeconds(.3),
                            AgentOutput()
                                    .withJump(true)
                                    .withThrottle(1.0)
                                    .withPitch(-1.0)))
                    .withStep(BlindStep(Duration.ofSeconds(.5),
                            AgentOutput()
                                    .withThrottle(1.0)
                                    .withPitch(-1.0)
                    ))
                    .withStep(LandGracefullyStep { it.agentInput.myCarData.velocity.flatten() })
        }
    }

}