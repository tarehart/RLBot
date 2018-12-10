package tarehart.rlbot.steps.defense

import rlbot.cppinterop.RLBotDll
import rlbot.flat.QuickChatSelection
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog

class WhatASaveStep : NestedPlanStep() {

    override fun getLocalSituation(): String {
        return "Making a save"
    }

    private var whichPost: Double? = null

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val ballPath = bundle.tacticalSituation.ballPath
        val goal = GoalUtil.getOwnGoal(bundle.agentInput.team)
        val currentThreat = TacticsTelemetry[bundle.agentInput.playerIndex]?.scoredOnThreat

        if (currentThreat == null) {
            RLBotDll.sendQuickChat(bundle.agentInput.playerIndex, false, QuickChatSelection.Compliments_WhatASave)
            return null
        }

        if (whichPost == null) {

            val carToThreat = currentThreat.space - car.position
            val carApproachVsBallApproach = carToThreat.flatten().correctionAngle(bundle.agentInput.ballVelocity.flatten())
            // When carApproachVsBallApproach < 0, car is to the right of the ball, angle wise. Right is positive X when we're on the positive Y side of the field.
            whichPost = Math.signum(-carApproachVsBallApproach * currentThreat.space.y)

        }

        val distance = VectorUtil.flatDistance(car.position, currentThreat.space)
        val plot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5.0), car.boost, distance - 15)


        val threatPosition = InterceptCalculator.getInterceptOpportunity(car, ballPath, plot)?.space ?: currentThreat.space

        val carToIntercept = threatPosition.minus(car.position)
        val carApproachVsBallApproach = carToIntercept.flatten().correctionAngle(bundle.agentInput.ballVelocity.flatten())

        if (Math.abs(carApproachVsBallApproach) > Math.PI / 5) {
            return startPlan(
                    Plan(Plan.Posture.SAVE).withStep(InterceptStep(Vector3(0.0, Math.signum(goal.center.y) * 1.5, 0.0))),
                    bundle)
        }

        BotLog.println("Flexible kick save... Good luck!", car.playerIndex)
        return startPlan(FirstViableStepPlan(Plan.Posture.SAVE)
                .withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
                .withStep(GetOnDefenseStep()), bundle)
    }
}
