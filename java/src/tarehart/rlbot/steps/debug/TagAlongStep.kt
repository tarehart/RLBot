package tarehart.rlbot.steps.debug

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.travel.ParkTheCarStep

import java.util.Optional

class TagAlongStep : NestedPlanStep() {

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {
        val p = Plan().withStep(ParkTheCarStep { inp ->
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
