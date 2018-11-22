package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.TacticsAdvisor

abstract class TacticalBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private lateinit var tacticsAdvisor: TacticsAdvisor

    override fun getOutput(input: AgentInput): AgentOutput {

        if (!::tacticsAdvisor.isInitialized) {
            tacticsAdvisor = getNewTacticsAdvisor()
        }
        val bundle = tacticsAdvisor.assessSituation(input, currentPlan)
        return getOutput(bundle)
    }

    /**
     * @param bundle TacticalBundle produced from tacticsAdvisor.assessSituation
     * @return AgentOutput to act on the car
     */
    open fun getOutput(bundle: TacticalBundle): AgentOutput {
        val input = bundle.agentInput
        val car = input.myCarData
        tacticsAdvisor.findMoreUrgentPlan(bundle, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = tacticsAdvisor.makeFreshPlan(bundle)
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

    abstract fun getNewTacticsAdvisor() : TacticsAdvisor

}