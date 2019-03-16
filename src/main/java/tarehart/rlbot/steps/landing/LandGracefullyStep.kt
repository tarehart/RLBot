package tarehart.rlbot.steps.landing

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.CarPredictor
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Mat3
import tarehart.rlbot.math.OrientationSolver
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.PI

class LandGracefullyStep(private val facingFn: (TacticalBundle) -> Vector2) : NestedPlanStep() {

    var wheelContactFrames = 0

    override fun getLocalSituation(): String {
        return "Landing gracefully"
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        if (car.hasWheelContact) {
            wheelContactFrames++
            if (wheelContactFrames > 5) {
                return null
            }
        }

        val carPredictor = CarPredictor(bundle.agentInput.playerIndex, false)

        val carMotion = carPredictor.predictCarMotion(bundle, Duration.ofSeconds(3.0))
        val impact = carMotion.getFirstPlaneBreak(ArenaModel.getCollisionPlanes())

        val renderer = BotLoopRenderer.forBotLoop(bundle.agentInput.bot)
        carMotion.renderIn3d(renderer)


        if (ArenaModel.isMicroGravity() && car.boost > 0 && impact == null ||
                impact?.time?.let { (it - car.time > Duration.ofSeconds(2.0)) } == true) {

            // If we're drifting in space, boost toward the nearest wall
            val nearestPlane = ArenaModel.getNearestPlane(car.position)
            return OrientationSolver.orientCar(car, Mat3.lookingTo(nearestPlane.normal * -1.0), 1.0 / 60).withBoost()
        }

        impact?.let {
            RenderUtil.drawSquare(renderer, Plane(it.direction, it.position), 5.0, Color.RED)

            if (it.direction.z == 0.0) {
                // It's a wall!
                val targetOrientation = Mat3.lookingTo(car.velocity.projectToPlane(it.direction), it.direction)
                return OrientationSolver.orientCar(bundle.agentInput.myCarData, targetOrientation, 1.0 / 60).withThrottle(1.0)
            }
//            else {
//                // It's not a wall!
//                val shouldWavedash = Math.abs(ArenaModel.GRAVITY) > 5 && impact.time != null && it.direction.z == 1.0
//                val time = impact.time
//
//                if (shouldWavedash && time != null) {
//                    val orientation = Mat3.lookingTo(facingFn.invoke(bundle).toVector3())
//                    return getWavedashOutput(bundle.agentInput.myCarData, orientation, time)
//                }
//            }
        }

        return OrientationSolver.orientCar(bundle.agentInput.myCarData, Mat3.lookingTo(facingFn.invoke(bundle).toVector3()), 1.0 / 60).withThrottle(1.0).withSlide(true)
    }

    fun getWavedashOutput(car: CarData, landingOrientation: Mat3, impactTime: GameTime): AgentOutput {
        // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.forward() ), Color.RED)
        // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.left() ), Color.GREEN)
        // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.up() ), Color.BLUE)
        // TODO: Take the table of velocities -> Angle from RocketScience for best angle of wavedash.
        // Currently wavedash is only forward dodge
        val orientation = /* Mat3.rotationMatrix(orientation.forward(), -PI/6) */ Mat3.rotationMatrix(landingOrientation.left(), -PI / 8) * landingOrientation // Roll
        // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.forward() ), Color.PINK)
        // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.left() ), Color.ORANGE)
        // RenderUtil.drawPath(renderer, listOf(input.myCarData.position, input.myCarData.position + orientation.up() ), Color.CYAN)
        val solution = OrientationSolver.orientCar(car, orientation, 1.0 / 60).withThrottle(1.0).withSlide(true)
        val timeUntil = impactTime - car.time
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
        val FACE_BALL = { bundle: TacticalBundle -> faceBall(bundle) }
        val FACE_MOTION = { bundle: TacticalBundle -> faceVelocity(bundle) }

        private fun faceBall(bundle: TacticalBundle): Vector2 {
            val toBall = bundle.agentInput.ballPosition.minus(bundle.agentInput.myCarData.position).flatten()
            return toBall.normalized()
        }

        private fun faceVelocity(bundle: TacticalBundle): Vector2 {
            val car = bundle.agentInput.myCarData
            if (car.velocity.isZero) {
                return car.orientation.noseVector.flatten()
            }
            return car.velocity.flatten().normalized()
        }
    }
}
