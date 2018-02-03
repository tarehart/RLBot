package tarehart.rlbot.steps.landing

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.rotation.PitchToPlaneStep
import tarehart.rlbot.steps.rotation.RollToPlaneStep
import tarehart.rlbot.steps.rotation.YawToPlaneStep
import tarehart.rlbot.steps.wall.DescendFromWallStep
import tarehart.rlbot.steps.wall.MountWallStep
import tarehart.rlbot.steps.wall.WallTouchStep

import java.awt.*

class LandGracefullyStep(private val facingFn: (AgentInput) -> Vector2) : NestedPlanStep() {


    override fun getLocalSituation(): String {
        return "Landing gracefully"
    }

    override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {
        return input.myCarData.position.z < NEEDS_LANDING_HEIGHT || ArenaModel.isBehindGoalLine(input.myCarData.position)
    }

    override fun doInitialComputation(input: AgentInput) {
        val car = input.myCarData
        if (ArenaModel.isCarOnWall(car) || ArenaModel.isNearFloorEdge(car)) {

            if (WallTouchStep.hasWallTouchOpportunity(input, ArenaModel.predictBallPath(input))) {
                startPlan(Plan().withStep(MountWallStep()).withStep(WallTouchStep()), input)
            }

            startPlan(Plan().withStep(DescendFromWallStep()), input)
        }
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {
        return startPlan(planRotation(input.myCarData, facingFn), input)
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
        private val SIN_45 = Math.sin(Math.PI / 4)
        val UP_VECTOR = Vector3(0.0, 0.0, 1.0)
        val NEEDS_LANDING_HEIGHT = .4
        val FACE_BALL = { inp: AgentInput -> faceBall(inp) }

        private fun faceBall(input: AgentInput): Vector2 {
            val toBall = input.ballPosition.minus(input.myCarData.position).flatten()
            return toBall.normalized()
        }

        private fun planRotation(car: CarData, facingFn: (AgentInput) -> Vector2): Plan {

            val current = car.orientation
            val pitchFirst = Math.abs(car.spin.pitchRate) > 1 || Math.abs(current.roofVector.z) > SIN_45

            return Plan()
                    .withStep(if (pitchFirst) PitchToPlaneStep(UP_VECTOR, true) else YawToPlaneStep({UP_VECTOR}, true))
                    .withStep(RollToPlaneStep(UP_VECTOR))
                    .withStep(YawToPlaneStep({ input -> getFacingPlane(facingFn.invoke(input)) }))
        }

        private fun getFacingPlane(desiredFacing: Vector2): Vector3 {
            val (x, y) = VectorUtil.rotateVector(desiredFacing.normalized(), -Math.PI / 2)
            return Vector3(x, y, 0.0)
        }
    }
}
