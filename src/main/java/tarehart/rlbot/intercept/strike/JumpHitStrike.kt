package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.LaunchChecklist
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.DirectedKickUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.LatencyAdvisor
import tarehart.rlbot.tuning.ManeuverMath

class JumpHitStrike(height: Double): StrikeProfile() {

    // If we have time to tilt back, the nose will be higher and we can cheat a little.
    private val requiredHeight = (height - StrikePlanner.CAR_BASE_HEIGHT) * .7

    override val preDodgeTime = Duration.ofSeconds(ManeuverMath.secondsForMashJumpHeight(requiredHeight).orElse(.8))
    override val postDodgeTime = Duration.ofMillis(150)
    override val speedBoost = 10.0
    override val style = Style.JUMP_HIT

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkJumpHitReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing JumpHit!", car.playerIndex)
            return performJumpHit(preDodgeTime.seconds)
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z < MAX_BALL_HEIGHT_FOR_JUMP_HIT
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Double): PreKickWaypoint? {
        val flatForce = desiredKickForce.flatten()
        val postDodgeSpeed = Math.min(AccelerationModel.SUPERSONIC_SPEED, expectedArrivalSpeed + speedBoost)
        val strikeTravel = preDodgeTime.seconds * expectedArrivalSpeed + postDodgeTime.seconds * postDodgeSpeed
        val launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(strikeTravel)
        return DirectedKickUtil.getStandardWaypoint(launchPosition, flatForce.normalized(), intercept)
    }

    private fun checkJumpHitReadiness(car: CarData, intercept: SpaceTime): LaunchChecklist {

        val checklist = LaunchChecklist()
        StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
        checklist.timeForIgnition = Duration.between(car.time + LatencyAdvisor.latency, intercept.time) < strikeDuration
        return checklist
    }

    companion object {

        const val MAX_BALL_HEIGHT_FOR_JUMP_HIT = ManeuverMath.MASH_JUMP_HEIGHT

        fun performJumpHit(jumpSeconds: Double): Plan {

            val pitchBackPortion = Math.min(.36, jumpSeconds)
            val driftUpPortion = jumpSeconds - pitchBackPortion

            val blindSequence = BlindSequence()



            blindSequence.withStep(BlindStep(Duration.ofSeconds(pitchBackPortion), AgentOutput()
                            .withJump(true)
                            .withPitch(1.0)
                    ))

            if (driftUpPortion > 0) {
                blindSequence.withStep(BlindStep(Duration.ofSeconds(driftUpPortion), AgentOutput().withJump(true)))
            }

            blindSequence
                    .withStep(BlindStep(Duration.ofMillis(16), AgentOutput()
                            .withPitch(-1.0)
                            .withJump(false)
                            .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(80), AgentOutput()
                            .withPitch(-1.0)
                            .withJump(true)
                            .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(800), AgentOutput()
                            .withThrottle(1.0)
                            .withPitch(-1.0)
                    ))

            val plan = Plan().unstoppable().withStep(blindSequence)
            return plan.withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }
    }

}