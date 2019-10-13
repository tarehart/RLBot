package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.tuning.BotLog.println

class ChaseBallStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        val tacticalSituation = TacticsTelemetry[bundle.agentInput.playerIndex]

        if (tacticalSituation?.expectedContact != null) {
            // There's an intercept, quit this thing.
            // TODO: sometimes the intercept step fails to pick up on this because its accelration model does no front flips.
            return null
        }

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
