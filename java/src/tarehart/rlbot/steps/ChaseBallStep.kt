package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.TacticalSituation
import tarehart.rlbot.planning.TacticsTelemetry

import java.awt.*
import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

class ChaseBallStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {
        val tacticalSituation = TacticsTelemetry[input.playerIndex]

        if (tacticalSituation?.expectedContact != null) {
            // There's an intercept, quit this thing.
            // TODO: sometimes the intercept step fails to pick up on this because its accelration model does no front flips.
            return Optional.empty()
        }

        val car = input.myCarData

        val sensibleFlip = SteerUtil.getSensibleFlip(car, input.ballPosition)
        if (sensibleFlip.isPresent) {
            println("Front flip after ball", input.playerIndex)
            return startPlan(sensibleFlip.get(), input)
        }

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.ballPosition))
    }

    override fun getLocalSituation(): String {
        return "Chasing ball"
    }
}
