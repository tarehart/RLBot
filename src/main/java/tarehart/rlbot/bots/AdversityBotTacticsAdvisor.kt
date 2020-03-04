package tarehart.rlbot.bots

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.GetOnOffenseStep
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.SaveAdvisor
import tarehart.rlbot.tactics.SoccerTacticsAdvisor
import tarehart.rlbot.tuning.BotLog

class AdversityBotTacticsAdvisor: SoccerTacticsAdvisor() {

    override fun makeFreshPlan(bundle: TacticalBundle): Plan {
        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        val plan = FirstViableStepPlan(Posture.NEUTRAL)

        if (situation.shotOnGoalAvailable && situation.teamPlayerWithInitiative?.car == car) {
            plan.withStep(FlexibleKickStep(KickAtEnemyGoal()))
        }

        plan.withStep(DemolishEnemyStep(isAdversityBot = true))
        plan.withStep(GetBoostStep())
        plan.withStep(GetOnOffenseStep())
        return plan
    }

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.SOCCER, GameMode.DROPSHOT, GameMode.HOOPS, GameMode.SPIKE_RUSH)
    }

    override fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff && situation.teamPlayerWithInitiative?.car == car) {
            return Plan(Posture.KICKOFF).withStep(GoForKickoffStep())
        }

        if (situation.scoredOnThreat != null && Posture.SAVE.canInterrupt(currentPlan)) {
            BotLog.println("Canceling current plan. Need to go for save!", input.playerIndex)
            return SaveAdvisor.planSave(bundle, situation.scoredOnThreat)
        }

        return null
    }
}
