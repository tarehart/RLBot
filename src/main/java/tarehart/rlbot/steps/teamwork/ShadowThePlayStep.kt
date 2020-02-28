package tarehart.rlbot.steps.teamwork

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.UnfailingStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.tactics.TacticalSituation
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class ShadowThePlayStep: NestedPlanStep(), UnfailingStep {

    override fun getLocalSituation(): String {
        return  "Shadowing the play"
    }

    override fun getUnfailingOutput(bundle: TacticalBundle): AgentOutput {
        // Currently super.getOutput will never return null, so the else condition won't be hit.
        return super.getOutput(bundle) ?: AgentOutput()
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

//        val ballFuture = situation.expectedContact?.space ?:
//        ballPath.getMotionAt(bundle.agentInput.time.plusSeconds(1.0))?.space ?: bundle.agentInput.ballPosition
        val ballFuture = bundle.agentInput.ballPosition

        val backoff = 75 + ballFuture.z
        var idealPosition = ballFuture.flatten().plus(Vector2(0, backoff * car.team.side))

        bundle.agentInput.getTeamRoster(car.team).forEach {
            if (it != car) {
                val teammateToMe = car.position - it.position
                idealPosition += teammateToMe.scaledToMagnitude(20.0).flatten()
            }
        }

        idealPosition = ArenaModel.clampPosition(idealPosition, 20.0)

        RenderUtil.drawSphere(car.renderer, idealPosition.withZ(1.0), 0.6, Color.WHITE)
        car.renderer.drawLine3d(Color.WHITE, car.position.toRlbot(), idealPosition.withZ(1.0).toRlbot())

        SteerUtil.getSensibleFlip(car, idealPosition)?.let {
            println("Front flip toward shadowing play", bundle.agentInput.playerIndex)
            return startPlan(it, bundle)
        }

        return SteerUtil.steerTowardGroundPosition(car, idealPosition, conserveBoost = true)
    }
}
