package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BotMath
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

class ParkTheCarStep(private val targetFunction: (AgentInput) -> PositionFacing?) : NestedPlanStep() {

    private var phase = AIM_AT_TARGET
    private var backwards: Boolean = false
    private var turnDirection: Int? = null
    private var target: PositionFacing? = null
    private var shouldSlide = false

    override fun getLocalSituation(): String {
        return "Parking the car"
    }

    private fun reasonableToGoBackwards(car: CarData) : Boolean {
        return car.velocity.flatten().dotProduct(car.orientation.noseVector.flatten()) < 15
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {

        val car = input.myCarData
        val latestTarget = targetFunction.invoke(input) ?: return Optional.empty()
        target = latestTarget
        val toTarget = latestTarget.position.minus(car.position.flatten())
        val distance = toTarget.magnitude()
        val facingCorrectionRadians = car.orientation.noseVector.flatten().correctionAngle(latestTarget.facing)
        val speed = car.velocity.magnitude()

        if (phase == AIM_AT_TARGET) {

            val noseToTargetAngle = Vector2.angle(toTarget, car.orientation.noseVector.flatten())
            val angle: Double
            if (backwards || toTarget.magnitude() < 22 &&
                    noseToTargetAngle > 5 * Math.PI / 6 &&
                    reasonableToGoBackwards(car)) {
                backwards = true
                angle = Math.PI - noseToTargetAngle
            } else {
                angle = noseToTargetAngle
            }

            if (distance < 4) {
                if (Math.abs(facingCorrectionRadians) < .1 && speed < 3) {
                    return Optional.empty() // We're already good
                }

                phase = SLIDE_SPIN
            }


            if (angle < Math.PI / 12) {
                phase = TRAVEL
            } else {
                if (backwards) {
                    return Optional.of(SteerUtil.backUpTowardGroundPosition(car, latestTarget.position))
                }
                val output = SteerUtil.steerTowardGroundPosition(car, input.boostData, latestTarget.position)
                if (distance < 20) {
                    output.withBoost(false)
                }
                return Optional.of(output)
            }
        }

        if (phase == TRAVEL) {


            val slideDistance = getSlideDistance(car.velocity.magnitude())

            if (distance < slideDistance) {
                phase = SLIDE_SPIN
            } else {

                if (backwards) {
                    return Optional.of(SteerUtil.backUpTowardGroundPosition(car, latestTarget.position))
                } else {

                    val correctionRadians = toTarget.correctionAngle(latestTarget.facing)

                    val offsetMagnitude = Math.min(10.0, Math.abs(correctionRadians) * 6)

                    val offsetVector = VectorUtil
                            .rotateVector(toTarget, -Math.signum(correctionRadians) * Math.PI / 2)
                            .scaledToMagnitude(offsetMagnitude)

                    val waypoint = latestTarget.position.plus(offsetVector)

                    val sensibleFlip = SteerUtil.getSensibleFlip(car, waypoint)
                    if (sensibleFlip.isPresent) {
                        return startPlan(sensibleFlip.get(), input)
                    }

                    return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.boostData, waypoint).withBoost(car.boost > 50 && distance > 20))
                }
            }
        }


        val turnDir = turnDirection ?: BotMath.nonZeroSignum(facingCorrectionRadians)
        turnDirection = turnDir

        shouldSlide = shouldSlide || Math.abs(facingCorrectionRadians) > Math.PI / 3 || distance < 4

        val futureRadians = facingCorrectionRadians + car.spin.yawRate * .3
        val steerPolarity = if (backwards) 1 else -1

        if (shouldSlide) {

            if (futureRadians * turnDir < 0 && Math.abs(futureRadians) < Math.PI / 4) {
                return Optional.empty() // Done orienting.
            }

            return Optional.of(AgentOutput()
                    .withDeceleration(if (backwards) 1.0 else 0.0)
                    .withAcceleration(if (backwards) 0.0 else 1.0)
                    .withSteer(turnDir.toDouble() * steerPolarity)
                    .withSlide())

        } else {

            if (futureRadians * turnDir < 0 && Math.abs(futureRadians) < Math.PI / 4) {
                return Optional.empty() // Done orienting.
            }

            val tooFast = distance < getBrakeDistance(car.velocity.magnitude())
            return Optional.of(AgentOutput()
                    .withAcceleration(if (tooFast == backwards) 1.0 else 0.0)
                    .withDeceleration(if (tooFast != backwards) 1.0 else 0.0)
                    .withSteer(turnDir.toDouble() * steerPolarity))
        }
    }

    private fun getSlideDistance(speed: Double): Double {
        return speed * speed * .015 + speed * .4
    }

    private fun getBrakeDistance(speed: Double): Double {
        return speed * speed * .01 + speed * .1
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
