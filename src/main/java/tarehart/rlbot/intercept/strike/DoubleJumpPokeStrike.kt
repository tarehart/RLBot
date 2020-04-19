package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.Interpolate
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

class DoubleJumpPokeStrike(heightOfContactPoint: Float): StrikeProfile() {

    override val preDodgeTime = Duration.ofSeconds(secondsForJumpTiltCornerHeight(heightOfContactPoint) ?: 1.4)
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

        val JUMP_HEIGHT_CURVE = listOf(
                Pair(0F, 1.1F), Pair(.2F, 2.6F), Pair(.48F, 6.7F), Pair(.71F, 9F), Pair(.95F, 10.5F), Pair(1.16F, 11.3F), Pair(1.4F, 11.58F))

        val MIN_JUMP = ReflexJumpStrike.JUMP_HEIGHT_CURVE[0].second

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
                    .withStep(BlindStep(Duration.ofMillis(100), AgentOutput()
                            .withPitch(1.0)
                            .withJump(true)
                            .withThrottle(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(120), AgentOutput()
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
