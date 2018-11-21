package tarehart.rlbot.steps.strikes

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog

class FinalApproachStep(private val kickPlan: DirectedKickPlan) : NestedPlanStep() {

    private var strikeStarted = false

    override fun getLocalSituation(): String {
        return "Final approach"
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        if (strikeStarted) {
            return null // If we're here, then we already launched and then finished our strike, so we're totally done.
        }

        if (!readyForFinalApproach(bundle.agentInput.myCarData, kickPlan.launchPad)) {
            BotLog.println("Failed final approach!", bundle.agentInput.playerIndex)
            return null
        }

        val car = bundle.agentInput.myCarData

        StrikePlanner.planImmediateLaunch(car, kickPlan.intercept)?.let {
            strikeStarted = true
            return startPlan(it, bundle)
        }

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5.0), 0.0)

        val steerPlan = kickPlan.launchPad.planRoute(car, distancePlot)

        val renderer = BotLoopRenderer.forBotLoop(bundle.agentInput.bot)
        kickPlan.renderDebugInfo(renderer)
        steerPlan.route.renderDebugInfo(renderer)

        return steerPlan.immediateSteer
    }

    companion object {
        fun readyForFinalApproach(car: CarData, launchPad: PreKickWaypoint) : Boolean {
            return launchPad.isPlausibleFinalApproach(car)
        }
    }
}
