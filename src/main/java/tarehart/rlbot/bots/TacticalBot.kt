package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.*

abstract class TacticalBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private lateinit var tacticsAdvisor: TacticsAdvisor
    private val bundleListeners = ArrayList<BundleListener>()

    override var loaded = false
        get() = super.loaded && ::tacticsAdvisor.isInitialized

    private fun assessSituation(input: AgentInput): TacticalBundle {
        if (!::tacticsAdvisor.isInitialized) {
            tacticsAdvisor = getNewTacticsAdvisor()
        }
        return tacticsAdvisor.assessSituation(input, currentPlan)
    }

    override fun getOutput(input: AgentInput): AgentOutput {
        val bundle = assessSituation(input)
        return getOutput(bundle)
    }

    /**
     * @param bundle TacticalBundle produced from tacticsAdvisor.assessSituation
     * @return AgentOutput to act on the car
     */
    open fun getOutput(bundle: TacticalBundle): AgentOutput {

        bundleListeners.forEach { it.processBundle(bundle) }

        val input = bundle.agentInput
        val car = input.myCarData
        tacticsAdvisor.findMoreUrgentPlan(bundle, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = tacticsAdvisor.makeFreshPlan(bundle)
        }

        currentPlan?.let {
            val planOutput = it.getOutput(bundle)
            if (planOutput != null) {
                return planOutput
            } else {
                currentPlan = null
            }
        }
        return SteerUtil.steerTowardGroundPosition(car, input.ballPosition.flatten()).withBoost(car.boost > 75)
    }

    open fun getNewTacticsAdvisor() : TacticsAdvisor {
        val gameMode = GameModeSniffer.getGameMode()

        return when (gameMode) {
            GameMode.SOCCER -> {
                println("Game Mode: Soccar")
                SoccerTacticsAdvisor()
            }
            GameMode.DROPSHOT -> {
                println("Game Mode: Dropshot")
                DropshotTacticsAdvisor()
            }
            GameMode.HOOPS -> {
                println("Game Mode: Hoops")
                HoopsTacticsAdvisor()
            }
            GameMode.SPIKE_RUSH -> {
                println("Game Mode: Spike Rush")
                SpikeRushTacticsAdvisor()
            }
        }
    }

    override fun roundInLimbo(input: AgentInput) {
        // Situation is assessed even when we can't control the car.
        // We may want the ability to control the wheels or something for aesthetic later
        // We would need to return an AgentOutput in that case.
        assessSituation(input)
    }

    fun registerBundleListener(bundleListener: BundleListener) {
        bundleListeners.add(bundleListener)
    }

}
