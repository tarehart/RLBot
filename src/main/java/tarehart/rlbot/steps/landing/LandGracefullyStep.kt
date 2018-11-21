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
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.PI

class LandGracefullyStep(private val facingFn: (AgentInput) -> Vector2) : NestedPlanStep() {



    override fun getLocalSituation(): String {
        return "Landing gracefully"
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        return input.myCarData.position.z < NEEDS_LANDING_HEIGHT || ArenaModel.isBehindGoalLine(input.myCarData.position)
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = input.myCarData

        if (car.hasWheelContact) {
            return null
        }

        val carPredictor = CarPredictor(input.playerIndex, false)

        val carMotion = carPredictor.predictCarMotion(input, Duration.ofSeconds(3.0))
        val impact = carMotion.getFirstPlaneBreak(ArenaModel.getCollisionPlanes())

        val renderer = BotLoopRenderer.forBotLoop(input.bot)
        carMotion.renderIn3d(renderer)



        impact?.let {
            RenderUtil.drawSquare(renderer, Plane(it.direction, it.position), 5.0, Color.RED)
            if (it.direction.z == 0.0) {
                // It's a wall!
                val targetOrientation = Mat3.lookingTo(Vector3(0.0, 0.0, -1.0), it.direction)
                return OrientationSolver.orientCar(input.myCarData, targetOrientation, 1.0 / 60).withThrottle(1.0)
            } else {
                // It's not a wall!
                impact.time?.let { // We know the impact time.
                    var orientation= Mat3.lookingTo(facingFn.invoke(input).toVector3())

                    // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.forward() ), Color.RED)
                    // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.left() ), Color.GREEN)
                    // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.up() ), Color.BLUE)
                    // TODO: Take the table of velocities -> Angle from RocketScience for best angle of wavedash.
                    // Currently wavedash is only forward dodge
                    orientation = /* Mat3.rotationMatrix(orientation.forward(), -PI/6) */ Mat3.rotationMatrix(orientation.left(), -PI/8) *  orientation // Roll
                    // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.forward() ), Color.PINK)
                    // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.left() ), Color.ORANGE)
                    // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.up() ), Color.CYAN)
                    val solution = OrientationSolver.orientCar(input.myCarData, orientation, 1.0 / 60).withThrottle(1.0).withSlide(true)
                    val timeUntil = impact.time - input.time
                    if (timeUntil.millis < 100) {
                        solution.withPitch(-1.0).withSteer(0.0).withJump(true)//.withBoost(true)
                    }
                    if (timeUntil.millis < 50) {
                        solution.withJump(false)
                    }
                    // val ballAngle = input.myCarData.orientation.noseVector.dotProduct(facingFn.invoke(input).toVector3())
                    // print("Vel, Impact ")
                    // print(input.myCarData.velocity.flatten().magnitude())
                    // print(", ")
                    // println(timeUntil.millis)
                    return solution
                }
            }
        }

        return OrientationSolver.orientCar(input.myCarData, Mat3.lookingTo(facingFn.invoke(input).toVector3()), 1.0 / 60).withThrottle(1.0).withSlide(true)
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
        val FACE_MOTION = { inp: AgentInput -> faceVelocity(inp) }

        private fun faceBall(bundle: TacticalBundle): Vector2 {
            val toBall = input.ballPosition.minus(input.myCarData.position).flatten()
            return toBall.normalized()
        }

        private fun faceVelocity(bundle: TacticalBundle): Vector2 {
            return input.myCarData.velocity.flatten().normalized()
        }
    }
}
