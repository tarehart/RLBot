package tarehart.rlbot.steps.defense

import rlbot.cppinterop.RLBotDll
import rlbot.flat.QuickChatSelection
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.CatchBallStep
import tarehart.rlbot.steps.DribbleStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.tuning.BotLog

class WhatASaveStep : NestedPlanStep() {

    override fun getLocalSituation(): String {
        return "Making a save"
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val currentThreat = bundle.tacticalSituation.scoredOnThreat

        if (currentThreat == null) {
            RLBotDll.sendQuickChat(bundle.agentInput.playerIndex, false, QuickChatSelection.Compliments_WhatASave)
            return null
        }


        BotLog.println("Flexible kick save... Good luck!", car.playerIndex)

        val plan = FirstViableStepPlan(Plan.Posture.SAVE).withStep(PlanarBlockStep())
                .withStep(FlexibleKickStep(KickAwayFromOwnGoal()))

        bundle.tacticalSituation.expectedContact?.let {
            val goalToContact = (it.space - GoalUtil.getOwnGoal(car.team).center).flatten()
            val contactToBall = (bundle.agentInput.ballPosition - it.space).flatten()
            if (goalToContact.dotProduct(contactToBall) > 0) {
                plan.withStep(InterceptStep())
            }
        }

        plan.withStep(CatchBallStep())

        return startPlan(plan, bundle)
    }
}
