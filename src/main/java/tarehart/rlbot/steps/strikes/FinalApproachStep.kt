package tarehart.rlbot.steps.strikes

import rlbot.render.NamedRenderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog

class FinalApproachStep(private val kickPlan: DirectedKickPlan) : NestedPlanStep() {

    private var strikeStarted = false
    private val disruptionMeter = BallPathDisruptionMeter(1.0)

    override fun getLocalSituation(): String {
        return "Final approach toward %s".format(kickPlan.launchPad.position)
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        if (strikeStarted) {
            return null // If we're here, then we already launched and then finished our strike, so we're totally done.
        }

        if (disruptionMeter.isDisrupted(bundle.tacticalSituation.ballPath)) {
            return null
        }

        if (!readyForFinalApproach(bundle.agentInput.myCarData, kickPlan.launchPad)) {
            if (kickPlan.intercept.strikeProfile.style == StrikeProfile.Style.CHIP) {
                BotLog.println("Probably chipped successfully!", bundle.agentInput.playerIndex)
            } else {
                BotLog.println("Failed final approach!", bundle.agentInput.playerIndex)
            }
            return null
        }

        val car = bundle.agentInput.myCarData

        kickPlan.intercept.strikeProfile.getPlan(car, kickPlan.intercept.ballSlice.toSpaceTime())?.let {
            strikeStarted = true
            BotLog.println("Launched at %s %s with speed %s"
                    .format(bundle.agentInput.time, car.position, car.velocity.magnitude()),
                    car.playerIndex)
            return startPlan(it, bundle)
        }

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5.0), 0.0)

        val steerPlan = kickPlan.launchPad.planRoute(car, distancePlot)

//        val renderer = NamedRenderer("finalApproach${car.playerIndex}")
//        renderer.startPacket()
//        kickPlan.renderDebugInfo(renderer)
//        steerPlan.route.renderDebugInfo(renderer)
//        renderer.finishAndSend()

        return steerPlan.immediateSteer
    }

    companion object {
        fun readyForFinalApproach(car: CarData, launchPad: PreKickWaypoint) : Boolean {
            return launchPad.isPlausibleFinalApproach(car)
        }
    }
}
