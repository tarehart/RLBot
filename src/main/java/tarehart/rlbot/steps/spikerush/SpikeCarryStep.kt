package tarehart.rlbot.steps.spikerush

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.tactics.SpikeRushTacticsAdvisor

class SpikeCarryStep : StandardStep() {

    override val situation: String
        get() = "Carrying spiked ball!"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        val distance = car.position.distance(bundle.agentInput.ballPosition)

        if (distance > SpikeRushTacticsAdvisor.SPIKED_DISTANCE) {
            return null
        }

        val scorePosition = GoalUtil.getEnemyGoal(car.team).getNearestEntrance(car.position, 2.0).flatten()
        return SteerUtil.steerTowardGroundPosition(car, scorePosition, false)
    }
}
