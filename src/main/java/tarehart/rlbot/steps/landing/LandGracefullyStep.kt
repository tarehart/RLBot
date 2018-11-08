package tarehart.rlbot.steps.landing

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.CarPredictor
import tarehart.rlbot.math.Mat3
import tarehart.rlbot.math.OrientationSolver
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.wall.DescendFromWallStep
import tarehart.rlbot.time.Duration
import java.awt.Color
import java.awt.Graphics2D

class LandGracefullyStep(private val facingFn: (AgentInput) -> Vector2) : NestedPlanStep() {



    override fun getLocalSituation(): String {
        return "Landing gracefully"
    }

    override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {
        return input.myCarData.position.z < NEEDS_LANDING_HEIGHT || ArenaModel.isBehindGoalLine(input.myCarData.position)
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData

        if (car.hasWheelContact) {
            return null
        }

        val carPredictor = CarPredictor(input.playerIndex, false)

        val carMotion = carPredictor.predictCarMotion(input, Duration.ofSeconds(3.0))
        val impact = carMotion.getFirstPlaneBreak(ArenaModel.getCollisonPlanes())

        val renderer = BotLoopRenderer.forBotLoop(input.bot)
        carMotion.renderIn3d(renderer)

        impact?.let {
            RenderUtil.drawSquare(renderer, Plane(it.normal, it.position), 5.0, Color.RED)
            if (it.normal.z == 0.0) {
                // It's a wall!
                val targetOrientation = Mat3.lookingTo(Vector3(0.0, 0.0, -1.0), it.normal)
                return OrientationSolver.orientCar(input.myCarData, targetOrientation, 1.0 / 60).withThrottle(1.0)
            }
        }

        return OrientationSolver.orientCar(input.myCarData, Mat3.lookingTo(facingFn.invoke(input).toVector3()), 1.0 / 60).withThrottle(1.0)
    }

    override fun canAbortPlanInternally(): Boolean {
        return true
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {
        val NEEDS_LANDING_HEIGHT = .4
        val FACE_BALL = { inp: AgentInput -> faceBall(inp) }

        private fun faceBall(input: AgentInput): Vector2 {
            val toBall = input.ballPosition.minus(input.myCarData.position).flatten()
            return toBall.normalized()
        }
    }
}
