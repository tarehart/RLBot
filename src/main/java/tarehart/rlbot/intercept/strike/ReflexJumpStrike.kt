package tarehart.rlbot.intercept.strike

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.LaunchChecklist
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.Interpolate
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.LatencyAdvisor
import tarehart.rlbot.tuning.ManeuverMath

class ReflexJumpStrike(private val heightOfContactPoint: Float): StrikeProfile() {

    override val preDodgeTime = Duration.ofSeconds(secondsForJumpTiltCornerHeight(heightOfContactPoint) ?: .8F) + ReflexStrikeStep.UNJUMP_TIME
    override val postDodgeTime = Duration.ofMillis(32)
    override val speedBoost = 10F
    override val style = Style.REFLEX_JUMP_HIT

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkJumpHitReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing Reflex Jump Hit!", car.playerIndex)
            val plan = Plan().unstoppable().withStep(ReflexStrikeStep(heightOfContactPoint, intercept.time.plusSeconds(.5)))
            return plan.withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z < MAX_JUMP
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

        // This was found experimentally by JumpTiltMeasurementTest
        // It represents a graph of secondsSinceJump vs heightOfHighestPontOnHitbox, when following a particular
        // sequence of pitches.
        val JUMP_HEIGHT_CURVE = listOf(
                Pair(0F, 1.1F), Pair(.18F, 2.55F), Pair(.33F, 4.14F), Pair(.45F, 5.02F), Pair(.66F, 5.89F), Pair(0.84F, 6.2F), Pair(0.9F, 6.2F))

        val MIN_JUMP = JUMP_HEIGHT_CURVE[0].second
        val MAX_JUMP = JUMP_HEIGHT_CURVE.last().second

        /**
         * The car starts on the ground. Presses and holds jump the entire time, and pitches backwards
         * to lift the nose upward. We track the height of the top-front edge of the hitbox, as an approximation
         * for where contact with the ball will likely occur. This should be calibrated to a particular pattern
         * of pitching.
         */
        fun secondsForJumpTiltCornerHeight(heightOfContactPoint: Float): Float? {
            if (heightOfContactPoint < MIN_JUMP) {
                return 0F
            }
            return Interpolate.getInverse(JUMP_HEIGHT_CURVE, heightOfContactPoint)
        }
    }

}
