package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.tactics.SoccerTacticsAdvisor
import tarehart.rlbot.tactics.TacticsTelemetry

class RotateAndWaitToClearStep : NestedPlanStep() {


    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val positionFacing = GetOnDefenseStep.calculatePositionFacing(bundle.agentInput)
        val car = bundle.agentInput.myCarData

        val positionError = car.position.flatten().distance(positionFacing.position)
        val facingError = Vector2.angle(car.orientation.noseVector.flatten(), positionFacing.facing)

        if (positionError < 5 && facingError < Math.PI / 8) {
            return AgentOutput()
        }

        val plan = Plan(Plan.Posture.DEFENSIVE)
                .withStep(ParkTheCarStep { inp -> GetOnDefenseStep.calculatePositionFacing(inp) })

        return startPlan(plan, bundle)
    }

    override fun getLocalSituation(): String {
        return "Rotating and waiting to clear"
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        val tacticalSituation = TacticsTelemetry[bundle.agentInput.myCarData.playerIndex]
        return !SoccerTacticsAdvisor.getWaitToClear(bundle, tacticalSituation?.enemyPlayerWithInitiative?.car)
                || tacticalSituation?.ballAdvantage?.seconds?.let { it > 0.8 } ?: false
    }

}


