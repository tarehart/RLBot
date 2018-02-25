package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.TacticsTelemetry

import tarehart.rlbot.tuning.BotLog.println

class ChaseBallStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {
        val tacticalSituation = TacticsTelemetry[input.playerIndex]

        if (tacticalSituation?.expectedContact != null) {
            // There's an intercept, quit this thing.
            // TODO: sometimes the intercept step fails to pick up on this because its accelration model does no front flips.
            return null
        }

        val car = input.myCarData

        SteerUtil.getSensibleFlip(car, input.ballPosition)?.let {
            println("Front flip after ball", input.playerIndex)
            return startPlan(it, input)
        }

        return SteerUtil.steerTowardGroundPosition(car, input.ballPosition)
    }

    override fun getLocalSituation(): String {
        return "Chasing ball"
    }
}
