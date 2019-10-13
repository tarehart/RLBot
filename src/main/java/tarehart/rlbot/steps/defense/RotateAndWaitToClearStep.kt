package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.tactics.SoccerTacticsAdvisor
import tarehart.rlbot.tactics.TacticsTelemetry

class RotateAndWaitToClearStep : NestedPlanStep() {

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        val tacticalSituation = bundle.tacticalSituation
        if (!SoccerTacticsAdvisor.getWaitToClear(bundle, tacticalSituation.enemyPlayerWithInitiative?.car)
                || tacticalSituation.ballAdvantage.seconds > 0.8) {
            return true
        }
        return false
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        val plan = Plan(Plan.Posture.DEFENSIVE)
                .withStep(ParkTheCarStep { inp -> GetOnDefenseStep.calculatePositionFacing(inp) })

        return startPlan(plan, bundle)
    }

    override fun getLocalSituation(): String {
        return "Rotating and waiting to clear"
    }
}


