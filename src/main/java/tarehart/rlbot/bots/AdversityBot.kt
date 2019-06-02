package tarehart.rlbot.bots

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.GetOnOffenseStep
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.defense.WhatASaveStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal
import tarehart.rlbot.tuning.BotLog

class AdversityBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex) {

    override fun getOutput(bundle: TacticalBundle): AgentOutput {

        val input = bundle.agentInput
        val car = input.myCarData

        findMoreUrgentPlan(bundle, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = makeFreshPlan(bundle)
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(bundle)?.let { return it }
            }
        }

        return SteerUtil.steerTowardGroundPosition(car, input.ballPosition.flatten()).withBoost(false)
    }

    private fun makeFreshPlan(bundle: TacticalBundle): Plan {
        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        val plan = FirstViableStepPlan(Plan.Posture.NEUTRAL)

        if (situation.shotOnGoalAvailable && situation.teamPlayerWithInitiative?.car == car) {
            plan.withStep(FlexibleKickStep(KickAtEnemyGoal()))
        }

        plan.withStep(DemolishEnemyStep(isAdversityBot = true))
        plan.withStep(GetBoostStep())
        plan.withStep(GetOnOffenseStep())
        return plan
    }

    private fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (Plan.Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff && situation.teamPlayerWithInitiative?.car == car) {
            return Plan(Plan.Posture.KICKOFF).withStep(GoForKickoffStep())
        }

        if (situation.scoredOnThreat != null && situation.teamPlayerWithInitiative?.car == car && Plan.Posture.SAVE.canInterrupt(currentPlan)) {
            BotLog.println("Canceling current plan. Need to go for save!", input.playerIndex)
            return Plan(Plan.Posture.SAVE).withStep(WhatASaveStep())
        }

        return null
    }
}
