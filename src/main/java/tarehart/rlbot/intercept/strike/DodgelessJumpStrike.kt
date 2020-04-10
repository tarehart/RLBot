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
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.DirectedKickUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.LatencyAdvisor
import tarehart.rlbot.tuning.ManeuverMath
import kotlin.math.min

class DodgelessJumpStrike(height: Float): StrikeProfile() {

    override val preDodgeTime = Duration.ofSeconds(ManeuverMath.secondsForMashJumpHeight(height) ?: .8F + .016F)
    override val postDodgeTime = Duration.ofMillis(0)
    override val speedBoost = 0F
    override val style = Style.DODGELESS_JUMP_HIT

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkJumpHitReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing Dodgeless Jump!", car.playerIndex)
            return performJumpHit(preDodgeTime.seconds)
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z < MAX_BALL_HEIGHT_FOR_JUMP_HIT
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Float): PreKickWaypoint? {
        val flatForce = desiredKickForce.flatten()
        return AnyFacingPreKickWaypoint(intercept.space.flatten(), flatForce.normalized(), 1F,
                expectedTime = intercept.time - strikeDuration,
                waitUntil = if (intercept.needsPatience) intercept.time else null)
    }

    private fun checkJumpHitReadiness(car: CarData, intercept: SpaceTime): LaunchChecklist {

        val checklist = LaunchChecklist()
        StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
        checklist.timeForIgnition = Duration.between(car.time + LatencyAdvisor.latency, intercept.time) < strikeDuration
        return checklist
    }

    companion object {

        const val MAX_BALL_HEIGHT_FOR_JUMP_HIT = ManeuverMath.MASH_JUMP_HEIGHT

        fun performJumpHit(preDodgeSeconds: Float): Plan {
            val jumpSeconds = preDodgeSeconds
            val blindSequence = BlindSequence()
            blindSequence.withStep(BlindStep(Duration.ofSeconds(jumpSeconds), AgentOutput().withJump(true)))
            val plan = Plan().unstoppable().withStep(blindSequence)
            return plan.withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }
    }

}
