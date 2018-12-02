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
import tarehart.rlbot.tuning.ManeuverMath

class SideHitStrike(height: Double): StrikeProfile() {

    // If we have time to tilt back, the nose will be higher and we can cheat a little.
    private val requiredHeight = height - StrikePlanner.CAR_BASE_HEIGHT

    override val preDodgeTime = Duration.ofSeconds(0.07 + ManeuverMath.secondsForMashJumpHeight(requiredHeight).orElse(.8))
    override val postDodgeTime = Duration.ofMillis(300)
    override val speedBoost = 10.0
    override val style = Style.SIDE_HIT
    override val isForward = false

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkSideHitReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing SideHit!", car.playerIndex)
            val toIntercept = intercept.space.flatten() - car.position.flatten()
            val left = car.orientation.noseVector.flatten().correctionAngle(toIntercept) > 0
            return jumpSideFlip(left, preDodgeTime)
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z < JumpHitStrike.MAX_BALL_HEIGHT_FOR_JUMP_HIT
    }

    private fun checkSideHitReadiness(car: CarData, intercept: SpaceTime): LaunchChecklist {

        val checklist = LaunchChecklist()
        StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
        checklist.linedUp = true // TODO: calculate this properly
        checklist.timeForIgnition = Duration.between(car.time + StrikePlanner.LATENCY_DURATION, intercept.time) < strikeDuration
        return checklist
    }

    companion object {
        fun jumpSideFlip(flipLeft: Boolean, rawJumpTime: Duration, hurry: Boolean = false): Plan {

            val jumpTime = if (rawJumpTime.millis < 0) Duration.ofMillis(0) else rawJumpTime

            return Plan()
                    .unstoppable()
                    .withStep(BlindStep(Duration.ofMillis(50 + jumpTime.millis), AgentOutput()
                            .withJump(true)
                            .withBoost(hurry)
                            .withThrottle((if (hurry) 1 else 0).toDouble())))
                    .withStep(BlindStep(Duration.ofMillis(20), AgentOutput()
                            .withThrottle(1.0)
                    ))
                    .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()
                            .withJump(true)
                            .withThrottle(1.0)
                            .withYaw((if (flipLeft) -1 else 1).toDouble())))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }
    }

}