package tarehart.rlbot.tactics

import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan

interface TacticsAdvisor {


    fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan?

    fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan

    fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation

    companion object {
        fun getYAxisWrongSidedness(input: AgentInput): Double {
            val (_, y) = GoalUtil.getOwnGoal(input.team).center
            val playerToBallY = input.ballPosition.y - input.myCarData.position.y
            return playerToBallY * Math.signum(y)
        }

        fun getYAxisWrongSidedness(car: CarData, ball: Vector3): Double {
            val center = GoalUtil.getOwnGoal(car.team).center
            val playerToBallY = ball.y - car.position.y
            return playerToBallY * Math.signum(center.y)
        }
    }


}
