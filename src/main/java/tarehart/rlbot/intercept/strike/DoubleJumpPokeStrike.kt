package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
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

class DoubleJumpPokeStrike(height: Float): StrikeProfile() {

    // TODO: secondsForMashJumpHeight is designed for single jumps not double jumps. It works pretty well anyway for now.
    override val preDodgeTime = Duration.ofSeconds(ManeuverMath.secondsForMashJumpHeight(height) ?: 0.8)
    override val postDodgeTime = Duration.ofMillis(0)
    override val speedBoost = 0.0F
    override val style = Style.DOUBLE_JUMP_POKE

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {

        val timeToGo = Duration.between(car.time + LatencyAdvisor.latency, intercept.time) < strikeDuration

        if (timeToGo) {
            BotLog.println("Performing double jump poke!", car.playerIndex)
            return performDoubleJumpPoke()
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z < MAX_BALL_HEIGHT_FOR_DOUBLE_JUMP_POKE
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Float): PreKickWaypoint? {
        val flatForce = desiredKickForce.flatten()
        val strikeTravel = preDodgeTime.seconds * expectedArrivalSpeed
        val launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(strikeTravel)
        return DirectedKickUtil.getStandardWaypoint(launchPosition, flatForce.normalized(), intercept)
    }

    companion object {

        const val MAX_BALL_HEIGHT_FOR_DOUBLE_JUMP_POKE = 11.5

        fun performDoubleJumpPoke(): Plan {

            val blindSequence = BlindSequence()
                    .withStep(BlindStep(Duration.ofMillis(200), AgentOutput()
                            .withJump(true)
                            .withPitch(0.5)
                    ))
                    .withStep(BlindStep(Duration.ofMillis(16), AgentOutput()
                            .withPitch(1.0)
                            .withJump(false)
                            .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()
                            .withJump(true)
                            .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(170), AgentOutput()
                            .withPitch(1.0)
                            .withJump(true)
                            .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()
                            .withPitch(-1.0)
                            .withJump(true)
                            .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(800), AgentOutput()
                            .withJump(true)
                            .withThrottle(1.0)))

            val plan = Plan().unstoppable().withStep(blindSequence)
            return plan.withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }
    }

}
