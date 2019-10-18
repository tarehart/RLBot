package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.LaunchChecklist
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

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Double): PreKickWaypoint? {
        val flatForce = desiredKickForce.flatten()
        val postDodgeSpeed = Math.min(AccelerationModel.SUPERSONIC_SPEED, expectedArrivalSpeed + speedBoost)
        val strikeTravel = preDodgeTime.seconds * expectedArrivalSpeed + postDodgeTime.seconds * postDodgeSpeed
        val launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(strikeTravel)
        return DirectedKickUtil.getStandardWaypoint(launchPosition, flatForce.normalized(), intercept)
    }

    private fun checkFlipHitReadiness(car: CarData, intercept: SpaceTime): LaunchChecklist {
        val checklist = LaunchChecklist()
        StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
        checklist.timeForIgnition = Duration.between(car.time + LatencyAdvisor.latency, intercept.time) <= strikeDuration
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
                    .withStep(BlindSequence()
                            .withStep(BlindStep(Duration.ofSeconds(.04),
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
                            )))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }
    }

}
