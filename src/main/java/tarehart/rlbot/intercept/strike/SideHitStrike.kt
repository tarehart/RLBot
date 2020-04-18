package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.LaunchChecklist
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.DirectedKickUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.LatencyAdvisor
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color
import kotlin.math.abs
import kotlin.math.sign

class SideHitStrike(height: Float): StrikeProfile() {

    private val jumpTime = ManeuverMath.secondsForMashJumpHeight(height - ArenaModel.BALL_RADIUS * .7F) ?: .8F
    override val preDodgeTime = Duration.ofSeconds(jumpTime + .04F)
    override val postDodgeTime = Duration.ofMillis(250)
    override val speedBoost = 10.0F
    override val style = Style.SIDE_HIT
    override val isForward = false

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkSideHitReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing SideHit!", car.playerIndex)
            val toIntercept = intercept.space.flatten() - car.position.flatten()
            val left = car.orientation.noseVector.flatten().correctionAngle(toIntercept) > 0
            return jumpSideFlip(left, Duration.ofSeconds(jumpTime))
        }
        if (checklist.timeForIgnition) {
            BotLog.println("Hesitating on side hit: $checklist", car.playerIndex)
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z < JumpHitStrike.MAX_BALL_HEIGHT_FOR_JUMP_HIT
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Float): PreKickWaypoint? {
        val flatForce = desiredKickForce.flatten()
        val estimatedApproachDeviationFromKickForce = DirectedKickUtil.getEstimatedApproachDeviationFromKickForce(
                car, intercept.space.flatten(), flatForce)

        val useFrontCorner = abs(estimatedApproachDeviationFromKickForce) < Math.PI * .45

        val carStrikeRadius = 1.0
        val carPositionAtContact = intercept.ballSlice.space - desiredKickForce.scaledToMagnitude(carStrikeRadius + ArenaModel.BALL_RADIUS)
        val anticipatedContactPoint = intercept.ballSlice.space - desiredKickForce.scaledToMagnitude(ArenaModel.BALL_RADIUS)
        val preKickRenderer = car.renderer

        RenderUtil.drawCircle(
                preKickRenderer,
                Circle(intercept.ballSlice.space.flatten(), ArenaModel.BALL_RADIUS),
                intercept.ballSlice.space.z,
                Color.RED)

        RenderUtil.drawSphereSlice(
                preKickRenderer,
                intercept.ballSlice.space,
                ArenaModel.BALL_RADIUS,
                anticipatedContactPoint.z,
                Color.RED)

        RenderUtil.drawCircle(
                preKickRenderer,
                Circle(carPositionAtContact.flatten(), carStrikeRadius),
                carPositionAtContact.z,
                Color.ORANGE)

        if (useFrontCorner) {

            val angled = DirectedKickUtil.getAngledWaypoint(intercept, expectedArrivalSpeed,
                    estimatedApproachDeviationFromKickForce, car.position.flatten(), carPositionAtContact.flatten(), Math.PI.toFloat() * .55F, car.renderer)

            if (angled == null) {
                BotLog.println("Failed to calculate side hit waypoint", car.playerIndex)
                return null
            }
            return angled
        } else {

            val facing = VectorUtil.rotateVector(
                    flatForce,
                    -sign(estimatedApproachDeviationFromKickForce) * Math.PI / 2)

            val postDodgeVelocity = intercept.strikeProfile.getPostDodgeVelocity(expectedArrivalSpeed)
            val lateralTravel = intercept.strikeProfile.postDodgeTime.seconds * postDodgeVelocity.sidewaysMagnitude

            // Consider the entire strike duration, not just the hang time.
            val backoff = intercept.strikeProfile.strikeDuration.seconds * expectedArrivalSpeed
            val launchPosition = carPositionAtContact.flatten() - flatForce.scaledToMagnitude(lateralTravel) -
                    facing.scaledToMagnitude(backoff)
            return DirectedKickUtil.getStandardWaypoint(launchPosition, facing, intercept)
        }
    }

    private fun checkSideHitReadiness(car: CarData, intercept: SpaceTime): LaunchChecklist {

        val checklist = LaunchChecklist()
        StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
        checklist.linedUp = true // TODO: calculate this properly
        checklist.timeForIgnition = Duration.between(car.time + LatencyAdvisor.latency, intercept.time) < strikeDuration
        return checklist
    }

    companion object {

        fun jumpSideFlip(flipLeft: Boolean, rawJumpTime: Duration, hurry: Boolean = false): Plan {

            val jumpTime = if (rawJumpTime.millis < 0) Duration.ofMillis(0) else rawJumpTime

            return Plan()
                    .unstoppable()
                    .withStep(BlindSequence()
                            .withStep(BlindStep(Duration.ofMillis(20 + jumpTime.millis), AgentOutput()
                                    .withJump(true)
                                    .withBoost(hurry)
                                    .withThrottle((if (hurry) 1 else 0).toDouble())))
                            .withStep(BlindStep(Duration.ofMillis(20), AgentOutput()
                                    .withThrottle(1.0)
                            ))
                            .withStep(BlindStep(Duration.ofMillis(20), AgentOutput()
                                    .withJump(true)
                                    .withThrottle(1.0)
                                    .withYaw((if (flipLeft) -1 else 1).toDouble()))))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }
    }

}
