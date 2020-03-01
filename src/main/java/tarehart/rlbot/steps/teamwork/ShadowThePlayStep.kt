package tarehart.rlbot.steps.teamwork

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.UnfailingStep
import tarehart.rlbot.tuning.BotLog.println
import java.awt.Color

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

        val ballPosition = bundle.agentInput.ballPosition
        val ballToOwnGoal = GoalUtil.getOwnGoal(car.team).center.flatten() - ballPosition.flatten()
        val backoffVectorMagnitude = (ballToOwnGoal.magnitude() - 5).coerceAtMost(70.0)
        var idealPosition = ballPosition.flatten() + ballToOwnGoal.scaledToMagnitude(backoffVectorMagnitude)

        bundle.agentInput.getTeamRoster(car.team).forEach {
            if (it != car) {
                val teammateToMe = car.position - it.position
                if (teammateToMe.magnitude() < 20) {
                    idealPosition += teammateToMe.scaledToMagnitude(20.0).flatten()
                }
            }
        }

        idealPosition = ArenaModel.clampPosition(idealPosition, 5.0)

        RenderUtil.drawSphere(car.renderer, idealPosition.withZ(1.0), 0.6, Color.WHITE)
        car.renderer.drawLine3d(Color.WHITE, car.position.toRlbot(), idealPosition.withZ(1.0).toRlbot())

        SteerUtil.getSensibleFlip(car, idealPosition)?.let {
            println("Front flip toward shadowing play", bundle.agentInput.playerIndex)
            return startPlan(it, bundle)
        }

        return SteerUtil.steerTowardGroundPosition(car, idealPosition, conserveBoost = true)
    }
}
