package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.Step

import java.awt.*
import java.awt.geom.Line2D
import java.util.Optional
import java.util.function.Function

class SlideToPositionStep(private val targetFunction: (AgentInput) -> PositionFacing?) : NestedPlanStep() {

    private var phase = AIM_AT_TARGET

    private var turnDirection: Int? = null

    private var target: PositionFacing? = null

    override fun getLocalSituation(): String {
        return "Sliding to position"
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {

        val targetOption = targetFunction.invoke(input) ?: return Optional.empty()

        target = targetOption

        val car = input.myCarData

        val toTarget = targetOption.position.minus(car.position.flatten())

        if (phase == AIM_AT_TARGET) {

            if (toTarget.magnitude() < 10) {
                return Optional.empty()
            }


            val angle = Vector2.angle(toTarget, car.orientation.noseVector.flatten())
            if (angle < Math.PI / 12) {
                phase = TRAVEL
            } else {
                return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.boostData, targetOption.position))
            }
        }

        if (phase == TRAVEL) {

            val distance = toTarget.magnitude()
            val slideDistance = getSlideDistance(car.velocity.magnitude())

            if (distance < slideDistance) {
                phase = SLIDE_SPIN
            } else {

                val correctionRadians = toTarget.correctionAngle(targetOption.facing)

                val offsetVector = VectorUtil
                        .rotateVector(toTarget, -Math.signum(correctionRadians) * Math.PI / 2)
                        .scaledToMagnitude(10.0)

                val waypoint = targetOption.position.plus(offsetVector)

                val sensibleFlip = SteerUtil.getSensibleFlip(car, waypoint)
                if (sensibleFlip.isPresent) {
                    return startPlan(sensibleFlip.get(), input)
                }

                return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.boostData, waypoint).withBoost(car.boost > 50))
            }
        }

        if (phase == SLIDE_SPIN) {
            val turnDir = turnDirection ?: Math.signum(car.orientation.noseVector.flatten().correctionAngle(toTarget)).toInt()
            turnDirection = turnDir

            val correctionRadians = car.orientation.noseVector.flatten().correctionAngle(targetOption.facing)
            val futureRadians = correctionRadians + car.spin.yawRate * .3

            return if (futureRadians * turnDir < 0 && Math.abs(futureRadians) < Math.PI / 4) {
                Optional.empty() // Done orienting.
            } else Optional.of(AgentOutput().withAcceleration(1.0).withSteer(-turnDir.toDouble()).withSlide())

        }

        return Optional.empty() // Something went wrong
    }

    private fun getSlideDistance(speed: Double): Double {
        return speed * .8
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        super.drawDebugInfo(graphics)

        target?.let {
            graphics.color = Color(75, 214, 214)
            graphics.stroke = BasicStroke(1f)
            val position = it.position
            val crossSize = 2
            graphics.draw(Line2D.Double(position.x - crossSize, position.y - crossSize, position.x + crossSize, position.y + crossSize))
            graphics.draw(Line2D.Double(position.x - crossSize, position.y + crossSize, position.x + crossSize, position.y - crossSize))
        }
    }

    companion object {

        private val AIM_AT_TARGET = 0
        private val TRAVEL = 1
        private val SLIDE_SPIN = 2
    }
}
