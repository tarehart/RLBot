package tarehart.rlbot.tactics

import rlbot.cppinterop.RLBotDll
import rlbot.flat.QuickChatSelection
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.RetryableViableStepPlan
import tarehart.rlbot.steps.CatchBallStep
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.defense.PlanarBlockStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.ManeuverMath

object SaveAdvisor {

    fun planSave(bundle: TacticalBundle, scoredOnThreat: BallSlice): Plan {
        val car = bundle.agentInput.myCarData
        val situation = bundle.tacticalSituation

        val savePlan = RetryableViableStepPlan(Posture.SAVE, "Making a save", GetOnDefenseStep()) {
            b -> b.tacticalSituation.scoredOnThreat != null
        }

        RLBotDll.sendQuickChat(car.playerIndex, false, QuickChatSelection.Reactions_Noooo)
        if (situation.ballAdvantage.seconds < 0 &&
                situation.expectedEnemyContact?.time?.isBefore(scoredOnThreat.time) == true &&
                situation.distanceBallIsBehindUs < 0) {
            BotLog.println("Need to save, but also need to challenge first!", car.playerIndex)
            savePlan.withStep(ChallengeStep())
        }

        savePlan.withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
        savePlan.withStep(PlanarBlockStep())
        savePlan.withStep(CatchBallStep())

        return savePlan
    }
}
