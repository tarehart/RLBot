package tarehart.rlbot.steps.debug

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.Step
import tarehart.rlbot.steps.travel.SlideToPositionStep
import tarehart.rlbot.time.Duration

import java.awt.*
import java.util.Optional

class TagAlongStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {
        val p = Plan().withStep(SlideToPositionStep { inp ->
            val enemyCarOption = inp.enemyCarData
            val enemyCar = enemyCarOption.get()

            val waypoint = enemyCar.position.plus(enemyCar.orientation.rightVector.scaled(4.0)).flatten()
            val targetFacing = enemyCar.orientation.noseVector.flatten()
            PositionFacing(waypoint, targetFacing)
        }).withStep(BlindStep(0.2, AgentOutput()))

        return startPlan(p, input)
    }

    override fun getLocalSituation(): String {
        return "Tagging along"
    }
}
