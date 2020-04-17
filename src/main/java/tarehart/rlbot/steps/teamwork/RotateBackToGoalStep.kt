package tarehart.rlbot.steps.teamwork

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.UnfailingStep
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color
import java.awt.Point
import kotlin.math.sign

/**
 * This is a step that will conclude when the bot arrives in the goal.
 * The bot will rotate to the far post, that is the post which is opposite of where
 * the threat is. It will not reevaluate the threat once the step has begun.
 */
class RotateBackToGoalStep: NestedPlanStep() {

    private var attractorNode: Vector2? = null

    override fun getLocalSituation(): String {
        return  "Rotating out"
    }

    override fun reset() {
        super.reset()
        attractorNode = null
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        if (attractorNode == null) {
            // This is where the ball will be a few seconds in the future.
            val threatLocation = bundle.tacticalSituation.futureBallMotion?.space ?: return null
            val side = -BotMath.nonZeroSignum(threatLocation.x)
            attractorNode = Vector2(NODE_MAGNITUDE_X * side, NODE_MAGNITUDE_Y * car.team.side)
        }

        val attractor = attractorNode ?: return null
        val carPosition = car.position.flatten()
        val toAttractor = attractor - carPosition
        val goalCenter = GoalUtil.getOwnGoal(car.team).center.flatten()
                .scaled(.92F) // A little bit in front of the goal
        val toGoalCenter = goalCenter - carPosition

        if (toGoalCenter.magnitude() < SoccerGoal.EXTENT) {
            // Done rotating out
            return null
        }

        val idealApproachToGoal = goalCenter - attractor
        val currentApproachToGoal = goalCenter - carPosition

        val angleError = currentApproachToGoal.correctionAngle(idealApproachToGoal) * -sign(attractor.x) * -car.team.side

        car.renderer.drawString2d("Angle error: $angleError", Color.WHITE, Point(100, 100), 2, 2)

        val attractorScale = angleError.coerceAtLeast(0F) * 20
        val goalCenterScale = (10 - angleError).coerceAtLeast(0F)

        val direction = toAttractor.scaledToMagnitude(attractorScale) + toGoalCenter.scaledToMagnitude(goalCenterScale)

        val safeFlipRange = toAttractor.magnitude() * .8F
        val waypoint = carPosition + direction
        val boostGreedWaypoint = BoostAdvisor.getBoostWaypoint(car, waypoint) ?: waypoint

        car.renderer.drawLine3d(Color.GREEN, car.position, waypoint.withZ(car.position.z))
        RenderUtil.drawSphere(car.renderer, attractor.withZ(3), 3, Color.BLUE)

        SteerUtil.getSensibleFlip(car, boostGreedWaypoint, safeFlipRange)?.let {
            println("Front flip toward rotating out", bundle.agentInput.playerIndex)
            return startPlan(it, bundle)
        }

        val steer = SteerUtil.steerTowardGroundPosition(car, boostGreedWaypoint, detourForBoost = false)

        if (car.velocity.magnitude() < AccelerationModel.MEDIUM_SPEED * .95) {
            steer.withBoost(false)
        }
        return steer
    }

    companion object {
        private val NODE_MAGNITUDE_X = 40F
        private val NODE_MAGNITUDE_Y = 80F
    }
}
