package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.TacticsAdvisor

abstract class TacticalBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private var tacticsAdvisor: TacticsAdvisor? = null

    override fun getOutput(input: AgentInput): AgentOutput {
        getNewTacticsAdvisor(tacticsAdvisor)?.let { this.tacticsAdvisor = it }
        val car = input.myCarData
        val ballPath = ArenaModel.predictBallPath(input)
        val bundle = tacticsAdvisor!!.assessSituation(input, ballPath, currentPlan)

        tacticsAdvisor!!.findMoreUrgentPlan(bundle, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = tacticsAdvisor!!.makeFreshPlan(bundle)
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(bundle)?.let { return it }
            }
        }

        return SteerUtil.steerTowardGroundPositionGreedily(car, input.ballPosition.flatten()).withBoost(false)
    }

    abstract fun getNewTacticsAdvisor(tacticsAdvisor: TacticsAdvisor?) : TacticsAdvisor?

}