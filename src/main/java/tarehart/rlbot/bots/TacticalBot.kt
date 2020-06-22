package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.tactics.*
import java.awt.Color

abstract class TacticalBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    protected lateinit var tacticsAdvisor: TacticsAdvisor
    private val bundleListeners = ArrayList<BundleListener>()
    protected var previousBundle: TacticalBundle? = null

    override var loaded = false
        get() = super.loaded && ::tacticsAdvisor.isInitialized

    private fun assessSituation(input: AgentInput): TacticalBundle {
        if (!::tacticsAdvisor.isInitialized) {
            tacticsAdvisor = getNewTacticsAdvisor(input)
        }
        return tacticsAdvisor.assessSituation(input, currentPlan)
    }

    override fun getOutput(input: AgentInput): AgentOutput {
        val bundle = assessSituation(input)
        val output = getOutput(bundle)
        previousBundle = bundle
        return output
    }

    /**
     * @param bundle TacticalBundle produced from tacticsAdvisor.assessSituation
     * @return AgentOutput to act on the car
     */
    open fun getOutput(bundle: TacticalBundle): AgentOutput {

        bundleListeners.forEach { it.processBundle(bundle) }

        val input = bundle.agentInput
        val car = input.myCarData

        CarSlice(car).render(car.renderer, Color.WHITE)
        bundle.agentInput.latestBallTouch?.let {
            RenderUtil.drawImpact(car.renderer, it.position, it.normal * 3, Color.ORANGE)
        }
        // RenderUtil.drawBallPath(car.renderer, bundle.tacticalSituation.ballPath, car.time.plusSeconds(6), Color.WHITE)

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

    open fun getNewTacticsAdvisor(input: AgentInput) : TacticsAdvisor {
        val gameMode = GameModeSniffer.getGameMode()

        return when (gameMode) {
            GameMode.SOCCER -> {
                println("Game Mode: Soccar")
                SoccerTacticsAdvisor(input)
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
            GameMode.HEATSEEKER -> {
                println("Game Mode: Heat Seeker")
                SoccerTacticsAdvisor(input)
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
