package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.routing.RoutePlanner
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class ParkTheCarStep(private val targetFunction: (AgentInput) -> PositionFacing?) : NestedPlanStep() {

    private var phase = AIM_AT_TARGET
    private var backwards: Boolean = false
    private var turnDirection: Int? = null
    private var target: PositionFacing? = null
    private var shouldManeuver = false

    override fun getLocalSituation(): String {
        return "Parking the car - phase $phase"
    }

    private fun reasonableToGoBackwards(car: CarData) : Boolean {
        return car.velocity.flatten().dotProduct(car.orientation.noseVector.flatten()) < 15
    }

    override fun doInitialComputation(bundle: TacticalBundle) {
        target?.let {
            val renderer = bundle.agentInput.myCarData.renderer
            RenderUtil.drawImpact(
                    renderer,
                    (it.position + it.facing.scaledToMagnitude(2.0)).withZ(0.4),
                    it.facing.scaledToMagnitude(4.0).withZ(0.4),
                    Color.WHITE)
        }


    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val input = bundle.agentInput
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

            if (distance < 5) {
                if (Math.abs(facingCorrectionRadians) < 1 && speed < 5) {
                    return null // We're already good
                }

                phase = MANEUVER
            }


            if (angle < Math.PI / 12) {
                phase = TRAVEL
            } else {
                if (backwards) {
                    return SteerUtil.backUpTowardGroundPosition(car, latestTarget.position)
                }
                val output = SteerUtil.steerTowardGroundPosition(car, latestTarget.position)
                if (distance < 20) {
                    output.withBoost(false)
                }
                return output
            }
        }

        if (phase == TRAVEL) {


            val slideDistance =
                    if (requiresSlide(facingCorrectionRadians))
                        speed * 1.0
                    else
                        speed * 0.2

            if (distance < slideDistance) {
                phase = MANEUVER
            } else {

                if (backwards) {
                    return SteerUtil.backUpTowardGroundPosition(car, latestTarget.position)
                } else {

                    val waypoint = latestTarget.position

                    if (waypoint.distance(flatPosition) > 60) {
                        SteerUtil.getSensibleFlip(car, waypoint)?.let { return startPlan(it, bundle) }
                    }

                    val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(6.0), car.boost)
                    val decelerationPeriod = RoutePlanner.getDecelerationDistanceWhenTargetingSpeed(flatPosition, waypoint, 30.0, distancePlot)

                    val steer = SteerUtil.steerTowardGroundPosition(car, waypoint, detourForBoost = distance > 50, conserveBoost = car.boost < 50 || distance < 20)

                    if (decelerationPeriod.distance + slideDistance >= distance && ManeuverMath.forwardSpeed(car) > 30) {
                        steer.withThrottle(-1.0)
                        steer.withBoost(false)
                    }

                    return steer
                }
            }
        }


        if (phase == MANEUVER) {


            val turnDir = turnDirection ?: BotMath.nonZeroSignum(facingCorrectionRadians)
            turnDirection = turnDir

            shouldManeuver = shouldManeuver || requiresSlide(facingCorrectionRadians)



            if (shouldManeuver) {

                phase++

                return startPlan(Plan(Posture.NEUTRAL)
                        .withStep(BlindStep(Duration.ofSeconds(0.2), AgentOutput()
                                .withThrottle(1.0)
                                .withSteer(turnDir.toDouble())))
                        .withStep(BlindStep(Duration.ofSeconds(0.05), AgentOutput()
                                .withThrottle(1.0)
                                .withSteer(-turnDir.toDouble())))
                        .withStep(SlideTillFacingStep(latestTarget.facing, turnDir)), bundle)
            } else {

                val futureRadians = facingCorrectionRadians + car.spin.yawRate * .3
                val steerPolarity = if (backwards) 1 else -1

                if (futureRadians * turnDir < 0 && Math.abs(futureRadians) < Math.PI / 4) {
                    return null // Done orienting.
                }

                val tooFast = distance < ManeuverMath.getBrakeDistance(car.velocity.magnitude())
                return AgentOutput()
                        .withThrottle(if (tooFast == backwards) 1.0 else -1.0)
                        .withSteer(turnDir.toDouble() * steerPolarity)
            }
        }

        val currentSpeed = ManeuverMath.forwardSpeed(car)
        if (Math.abs(currentSpeed) < 1) {
            return null
        }
        return AgentOutput().withThrottle(-0.5 * currentSpeed)
    }

    private fun requiresSlide(facingCorrectionRadians: Double) = Math.abs(facingCorrectionRadians) > Math.PI / 3

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
        private val MANEUVER = 2
    }
}
