package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.RoutePlanner
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

import java.awt.*
import java.awt.geom.Line2D

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

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        val latestTarget = targetFunction.invoke(input) ?: return null
        target = latestTarget
        val flatPosition = car.position.flatten()
        val toTarget = latestTarget.position.minus(flatPosition)
        val distance = toTarget.magnitude()
        val facingCorrectionRadians = car.orientation.noseVector.flatten().correctionAngle(latestTarget.facing)
        val speed = car.velocity.magnitude()

        if (phase == AIM_AT_TARGET) {

            val noseToTargetAngle = Vector2.angle(toTarget, car.orientation.noseVector.flatten())
            val angle: Double
            if (backwards || toTarget.magnitude() < 35 &&
                    noseToTargetAngle > 5 * Math.PI / 6 &&
                    reasonableToGoBackwards(car)) {
                backwards = true
                angle = Math.PI - noseToTargetAngle
            } else {
                angle = noseToTargetAngle
            }

            if (distance < 4) {
                if (Math.abs(facingCorrectionRadians) < .1 && speed < 3) {
                    return null // We're already good
                }

                phase = SLIDE_SPIN
            }


            if (angle < Math.PI / 12) {
                phase = TRAVEL
            } else {
                if (backwards) {
                    return SteerUtil.backUpTowardGroundPosition(car, latestTarget.position)
                }
                val output = SteerUtil.steerTowardGroundPositionGreedily(car, latestTarget.position)
                if (distance < 20) {
                    output.withBoost(false)
                }
                return output
            }
        }

        if (phase == TRAVEL) {


            val slideDistance = Math.min(AccelerationModel.MEDIUM_SPEED, getSlideDistance(car.velocity.magnitude()))

            if (distance < slideDistance) {
                phase = SLIDE_SPIN
            } else {

                if (backwards) {
                    return SteerUtil.backUpTowardGroundPosition(car, latestTarget.position)
                } else {

                    val correctionRadians = toTarget.correctionAngle(latestTarget.facing)

                    val offsetMagnitude = Math.min(10.0, Math.abs(correctionRadians) * 6)

                    val offsetVector = VectorUtil
                            .rotateVector(toTarget, -Math.signum(correctionRadians) * Math.PI / 2)
                            .scaledToMagnitude(offsetMagnitude)

                    val waypoint = latestTarget.position.plus(offsetVector)

                    SteerUtil.getSensibleFlip(car, waypoint)?.let { return startPlan(it, input) }

                    val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(6.0), car.boost)
                    val decelerationPeriod = RoutePlanner.getDecelerationDistanceWhenTargetingSpeed(flatPosition, waypoint, AccelerationModel.MEDIUM_SPEED, distancePlot)

                    val steer = SteerUtil.steerTowardGroundPositionGreedily(car, waypoint).withBoost(car.boost > 50 && distance > 20)

                    if (decelerationPeriod.distance >= distance) {
                        steer.withThrottle(-1.0)
                        steer.withBoost(false)
                    }

                    return steer
                }
            }
        }


        if (phase == SLIDE_SPIN) {


            val turnDir = turnDirection ?: BotMath.nonZeroSignum(facingCorrectionRadians)
            turnDirection = turnDir

            shouldSlide = shouldSlide || Math.abs(facingCorrectionRadians) > Math.PI / 3 || distance < 4

            val futureRadians = facingCorrectionRadians + car.spin.yawRate * .3
            val steerPolarity = if (backwards) 1 else -1

            if (shouldSlide) {

                if (futureRadians * turnDir < 0 && Math.abs(futureRadians) < Math.PI / 4) {
                    return null // Done orienting.
                }

                return AgentOutput()
                        .withThrottle(if (backwards) -1.0 else 1.0)
                        .withSteer(turnDir.toDouble() * steerPolarity)
                        .withSlide()

            } else {

                if (futureRadians * turnDir < 0 && Math.abs(futureRadians) < Math.PI / 4) {
                    return null // Done orienting.
                }

                val tooFast = distance < ManeuverMath.getBrakeDistance(car.velocity.magnitude())
                return AgentOutput()
                        .withThrottle(if (tooFast == backwards) 1.0 else -1.0)
                        .withSteer(turnDir.toDouble() * steerPolarity)
            }
        }

        // Braking phase

        val currentSpeed = ManeuverMath.forwardSpeed(car)
        if (Math.abs(currentSpeed) < 1) {
            return null
        }
        return AgentOutput().withThrottle(-0.5 * currentSpeed)
    }

    private fun getSlideDistance(speed: Double): Double {
        return speed * speed * .015 + speed * .4
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
