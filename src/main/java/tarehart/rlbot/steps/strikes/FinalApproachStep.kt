package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.Style
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import java.awt.Color

class FinalApproachStep(private val kickPlan: DirectedKickPlan) : NestedPlanStep() {

    private var strikeStarted = false
    private val disruptionMeter = BallPathDisruptionMeter(1.0)

    override fun getLocalSituation(): String {
        return "Final approach toward %s".format(kickPlan.launchPad.position)
    }

    override fun doInitialComputation(bundle: TacticalBundle) {
        val intercept = kickPlan.intercept
        val car = bundle.agentInput.myCarData
        RenderUtil.drawCircle(
                car.renderer,
                Circle(intercept.ballSlice.space.flatten(), ArenaModel.BALL_RADIUS.toDouble()),
                intercept.ballSlice.space.z,
                Color.RED)

        val anticipatedContactPoint = intercept.ballSlice.space - kickPlan.plannedKickForce.scaledToMagnitude(ArenaModel.BALL_RADIUS)

        RenderUtil.drawImpact(car.renderer, anticipatedContactPoint, kickPlan.plannedKickForce, Color.RED)

        RenderUtil.drawSquare(car.renderer, Plane(Vector3.UP, intercept.space), 1.0, Color.WHITE)
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        if (strikeStarted) {
            return null // If we're here, then we already launched and then finished our strike, so we're totally done.
        }

        if (disruptionMeter.isDisrupted(bundle.tacticalSituation.ballPath)) {
            return null
        }

        if (!readyForFinalApproach(bundle.agentInput.myCarData, kickPlan.launchPad)) {
            if (kickPlan.intercept.strikeProfile.style == Style.CHIP) {
                BotLog.println("Probably chipped successfully!", bundle.agentInput.playerIndex)
            } else {
                BotLog.println("Failed final approach!", bundle.agentInput.playerIndex)
            }
            return null
        }

        val car = bundle.agentInput.myCarData

        kickPlan.intercept.strikeProfile.getPlanFancy(car, kickPlan)?.let {
            strikeStarted = true
            BotLog.println("Launched at %s %s with speed %s"
                    .format(bundle.agentInput.time, car.position, car.velocity.magnitude()),
                    car.playerIndex)
            return startPlan(it, bundle)
        }

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5.0), 0F)

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
