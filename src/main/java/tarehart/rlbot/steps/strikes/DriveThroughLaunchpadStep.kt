package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import java.awt.Color

class DriveThroughLaunchpadStep(private val launchPad: PreKickWaypoint) : StandardStep() {

    override val situation = "Driving through launchpad"



    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        if (!launchPad.isPlausibleFinalApproach(car)) {
            BotLog.println("Finished driving through launchpad!", car.playerIndex)
            return null
        }

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5.0), 0F)
        val steerPlan = launchPad.planRoute(car, distancePlot)

        val renderer = car.renderer
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, launchPad.position.toVector3()), 2.0, Color.WHITE)
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, launchPad.position.toVector3()), 1.0, Color.WHITE)
        steerPlan.route.renderDebugInfo(renderer)

        return steerPlan.immediateSteer
    }
}
