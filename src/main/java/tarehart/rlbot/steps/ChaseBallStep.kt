package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.tuning.BotLog.println

class ChaseBallStep : NestedPlanStep(), UnfailingStep {

    override fun getUnfailingOutput(bundle: TacticalBundle): AgentOutput {
        return super.getOutput(bundle) ?: return SteerUtil.steerTowardGroundPosition(
                bundle.agentInput.myCarData, bundle.agentInput.ballPosition.flatten(), detourForBoost = true)
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        val car = bundle.agentInput.myCarData

        SteerUtil.getSensibleFlip(car, bundle.agentInput.ballPosition)?.let {
            println("Front flip after ball", bundle.agentInput.playerIndex)
            return startPlan(it, bundle)
        }

        return SteerUtil.steerTowardGroundPosition(car, bundle.agentInput.ballPosition.flatten(), detourForBoost = true)
    }

    override fun getLocalSituation(): String {
        return "Chasing ball"
    }
}
