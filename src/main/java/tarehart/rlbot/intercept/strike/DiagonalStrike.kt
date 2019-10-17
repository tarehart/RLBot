package tarehart.rlbot.intercept.strike

import rlbot.render.NamedRenderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.LaunchChecklist
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.DirectedKickPlan
import tarehart.rlbot.steps.strikes.DirectedKickUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.LatencyAdvisor
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

/**
 * @param height the height that the car should be at contact.
 */
class DiagonalStrike(height: Double): StrikeProfile() {

    private val jumpTime = ManeuverMath.secondsForMashJumpHeight(height) ?: .8
    override val preDodgeTime = Duration.ofSeconds(jumpTime + .02)
    override val postDodgeTime = Duration.ofMillis(60)
    override val speedBoost = 10.0
    override val style = Style.DIAGONAL_HIT
    override val isForward = false

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkDiagonalHitReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing DiagonalHit!", car.playerIndex)
            val toIntercept = intercept.space.flatten() - car.position.flatten()
            val left = car.velocity.flatten().correctionAngle(toIntercept) > 0
            return diagonalKick(left, Duration.ofSeconds(jumpTime))
        }
        return null
    }

    override fun getPlanFancy(car: CarData, kickPlan: DirectedKickPlan): Plan? {
        val checklist = checkDiagonalHitReadiness(car, kickPlan.intercept.toSpaceTime())
        checklist.linedUp = Math.abs(kickPlan.launchPad.facing.correctionAngle(car.orientation.noseVector.flatten())) < .1
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing DiagonalHit!", car.playerIndex)
            val toIntercept = kickPlan.intercept.space.flatten() - car.position.flatten()
            val left = car.velocity.flatten().correctionAngle(toIntercept) > 0
            return diagonalKick(left, Duration.ofSeconds(jumpTime))
        }
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z < JumpHitStrike.MAX_BALL_HEIGHT_FOR_JUMP_HIT
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Double): PreKickWaypoint? {
        val flatForce = desiredKickForce.flatten()
        val estimatedApproachDeviationFromKickForce = DirectedKickUtil.getEstimatedApproachDeviationFromKickForce(
                car, intercept.space.flatten(), flatForce)

        val carStrikeRadius = 1.5 + 0.2 * Clamper.clamp(Math.cos(estimatedApproachDeviationFromKickForce), 0.0, 1.0)

        // intercept.space is already supposed to be the car position at contact, BUT it's based on a hard-coded
        // default value for carStrikeRadius.
        val carPositionAtContact = intercept.ballSlice.space - desiredKickForce.scaledToMagnitude(carStrikeRadius + ArenaModel.BALL_RADIUS)
        val anticipatedContactPoint = intercept.ballSlice.space - desiredKickForce.scaledToMagnitude(ArenaModel.BALL_RADIUS)

        val preKickRenderer = NamedRenderer("diagonalKick")
        preKickRenderer.startPacket()

        val angled = DirectedKickUtil.getAngledWaypoint(intercept, expectedArrivalSpeed,
                estimatedApproachDeviationFromKickForce, car.position.flatten(), carPositionAtContact.flatten(),
                Math.PI / 4, preKickRenderer)

        if (angled == null) {
            BotLog.println("Failed to calculate diagonal waypoint", car.playerIndex)
            return null
        }

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

        preKickRenderer.finishAndSend()

        return angled
    }

    private fun checkDiagonalHitReadiness(car: CarData, intercept: SpaceTime): LaunchChecklist {

        val checklist = LaunchChecklist()
        StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
        checklist.linedUp = true // TODO: calculate this properly
        checklist.timeForIgnition = Duration.between(car.time + LatencyAdvisor.latency, intercept.time) < strikeDuration
        return checklist
    }

    companion object {

        fun diagonalKick(flipLeft: Boolean, rawJumpTime: Duration): Plan {

            val jumpTime = if (rawJumpTime.millis < 0) Duration.ofMillis(0) else rawJumpTime
            val tiltRate = 1.0 // 0.3 / jumpTime.seconds

            val tiltSeconds = Math.min(jumpTime.seconds, 0.1)

            return Plan()
                    .unstoppable()
                    .withStep(BlindSequence()
                            .withStep(BlindStep(Duration.ofSeconds(tiltSeconds), AgentOutput()
                                    .withJump(true)
                                    .withPitch(tiltRate)
                                    .withYaw(if (flipLeft) -tiltRate else tiltRate)
                                    .withRoll(if (flipLeft) tiltRate else -tiltRate)))
                            .withStep(BlindStep(jumpTime - Duration.ofSeconds(tiltSeconds), AgentOutput()
                                    .withJump(true)))
                            .withStep(BlindStep(Duration.ofMillis(20), AgentOutput()
                                    .withThrottle(1.0)
                            ))
                            .withStep(BlindStep(Duration.ofMillis(100), AgentOutput()
                                    .withJump(true)
                                    .withThrottle(1.0)
                                    .withPitch(-1.0)
                                    .withYaw((if (flipLeft) -1 else 1).toDouble())))
                            .withStep(BlindStep(Duration.ofMillis(700), AgentOutput())))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }
    }

}
