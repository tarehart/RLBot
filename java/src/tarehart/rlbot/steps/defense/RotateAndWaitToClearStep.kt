package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.planning.TacticsTelemetry
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.travel.ParkTheCarStep

import java.util.Optional

class RotateAndWaitToClearStep : NestedPlanStep() {

    private val centerOffset = -2.0
    private val awayFromGoal = -2.0


    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {

        val positionFacing = calculatePositionFacing(input)
        val car = input.myCarData

        val positionError = car.position.flatten().distance(positionFacing.position)
        val facingError = Vector2.angle(car.orientation.noseVector.flatten(), positionFacing.facing)

        if (positionError < 5 && facingError < Math.PI / 8) {
            return Optional.of(AgentOutput())
        }

        val plan = Plan(Plan.Posture.DEFENSIVE)
                .withStep(ParkTheCarStep { inp -> calculatePositionFacing(inp) })

        return startPlan(plan, input)
    }

    private fun calculatePositionFacing(inp: AgentInput): PositionFacing {
        val center = GoalUtil.getOwnGoal(inp.team).center
        val futureBallPosition = TacticsTelemetry[inp.playerIndex]?.futureBallMotion?.space ?: inp.ballPosition

        val targetPosition = Vector2(
                Math.signum(futureBallPosition.x) * centerOffset,
                center.y - Math.signum(center.y) * awayFromGoal)

        val targetFacing = Vector2(-Math.signum(targetPosition.x), 0.0)
        return PositionFacing(targetPosition, targetFacing)
    }

    override fun getLocalSituation(): String {
        return "Rotating and waiting to clear"
    }

    override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {
        return TacticsTelemetry[input.myCarData.playerIndex]?.waitToClear == false
    }

}


