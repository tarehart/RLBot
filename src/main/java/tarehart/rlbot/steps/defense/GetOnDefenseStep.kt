package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.UnfailingStep
import tarehart.rlbot.steps.teamwork.RotateBackToGoalStep
import tarehart.rlbot.steps.travel.AchieveVelocityStep

class GetOnDefenseStep : NestedPlanStep(), UnfailingStep {
    override fun getLocalSituation(): String {
        return "Getting on defense"
    }


    override fun getUnfailingOutput(bundle: TacticalBundle): AgentOutput {
        // Currently super.getOutput will never return null, so the else condition won't be hit.
        return super.getOutput(bundle) ?: AgentOutput()
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val ownGoal = GoalUtil.getOwnGoal(car.team)
        val goalCenter = ownGoal.center.flatten()
                .scaled(.95F) // A little bit in front of the goal

        val toGoalCenter = goalCenter - car.position.flatten()

        if (toGoalCenter.magnitude() < SoccerGoal.EXTENT) {

            val threatLocation = bundle.tacticalSituation.futureBallMotion?.space ?: return null
            val side = BotMath.nonZeroSignum(threatLocation.x)

            val plan = Plan(Posture.DEFENSIVE)
                    .withStep(AchieveVelocityStep(Vector3(side * .00001, 0, 0)))
            return startPlan(plan, bundle)
        } else {
            return startPlan(Plan(Posture.DEFENSIVE).withStep(RotateBackToGoalStep()), bundle)
        }
    }
}
