package tarehart.rlbot.steps.landing

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Mat3
import tarehart.rlbot.math.OrientationSolver
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
import tarehart.rlbot.steps.wall.WallTouchStep
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
