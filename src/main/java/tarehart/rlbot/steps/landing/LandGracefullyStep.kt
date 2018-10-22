package tarehart.rlbot.steps.landing

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.Mat3
import tarehart.rlbot.math.OrientationSolver
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.wall.DescendFromWallStep
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
        if (ArenaModel.isCarOnWall(car) || car.hasWheelContact && ArenaModel.isNearFloorEdge(car.position)) {

            return startPlan(Plan().withStep(DescendFromWallStep()), input)
        }

        //return startPlan(planRotation(input.myCarData, facingFn), input)

        if (car.hasWheelContact) {
            return null
        }

        return OrientationSolver.step(input.myCarData, Mat3.lookingTo(facingFn.invoke(input).toVector3()), 1.0 / 60).withThrottle(1.0)
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
