package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.RetryableViableStepPlan
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.GetOnOffenseStep
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.steps.teamwork.RotateBackToGoalStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.GameModeSniffer
import tarehart.rlbot.tactics.SaveAdvisor
import tarehart.rlbot.tactics.SoccerTacticsAdvisor
import tarehart.rlbot.tuning.BotLog

class AdversityBotTacticsAdvisor(input: AgentInput): SoccerTacticsAdvisor(input) {

    override fun makeFreshPlan(bundle: TacticalBundle): Plan {
        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        val plan = FirstViableStepPlan(Posture.NEUTRAL)

        if (situation.shotOnGoalAvailable && situation.teamPlayerWithInitiative?.car == car &&
                situation.expectedContact.intercept != null && situation.expectedContact.intercept.space.distance(car.position) < 50) {
            plan.withStep(FlexibleKickStep(KickAtEnemyGoal()))
        }
        plan.withStep(DemolishEnemyStep(isAdversityBot = true))

        if (GameModeSniffer.getGameMode() != GameMode.HEATSEEKER) {
            plan.withStep(GetBoostStep())
            plan.withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
            plan.withStep(RotateBackToGoalStep())
        }
        return plan
    }

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.SOCCER, GameMode.DROPSHOT, GameMode.HOOPS, GameMode.SPIKE_RUSH, GameMode.HEATSEEKER)
    }

    override fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        kickoffAdvisor.gradeKickoff(bundle)

        if (Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff) {
            val kickoffAdvice = kickoffAdvisor.giveAdvice(GoForKickoffStep.getKickoffType(car), bundle)
            return GoForKickoffStep.chooseKickoffPlan(bundle, kickoffAdvice)
        }

        if (situation.scoredOnThreat != null && Posture.SAVE.canInterrupt(currentPlan) &&
                GameModeSniffer.getGameMode() != GameMode.HEATSEEKER) {
            BotLog.println("Canceling current plan. Need to go for save!", input.playerIndex)
            return SaveAdvisor.planSave(bundle, situation.scoredOnThreat)
        }

        return null
    }
}
