package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.strike.FlipHitStrike
import tarehart.rlbot.intercept.strike.Style
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.cancellation.InterceptDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class InterceptStep(
        private val interceptModifier: Vector3 = Vector3(),
        private val needsChallenge: Boolean = true
) : NestedPlanStep() {
    override fun getLocalSituation(): String {
        return  "Working on intercept"
    }

    private var chosenIntercept: Intercept? = null
    private val interceptDisruptionMeter = InterceptDisruptionMeter(distanceThreshold = 10.0, timeThreshold = Duration.ofSeconds(1))

    override fun reset() {
        super.reset()
        interceptDisruptionMeter.reset(null)
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val carData = bundle.agentInput.myCarData
        val ballPath = bundle.tacticalSituation.ballPath
        val fullAcceleration = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(7.0), carData.boost, 0.0)

        val soonestIntercept = getSoonestIntercept(carData, ballPath, fullAcceleration, interceptModifier)
        if (soonestIntercept == null) {
            println("No intercept option found, aborting.", bundle.agentInput.playerIndex)
            return null
        }

        chosenIntercept = soonestIntercept

        val launchPlan = soonestIntercept.strikeProfile.getPlan(carData, soonestIntercept.toSpaceTime())

        launchPlan?.let {
            it.unstoppable()
            return startPlan(it, bundle)
        }

        if (interceptDisruptionMeter.isDisrupted(soonestIntercept.ballSlice)) {
            println("Intercept has diverged from expectation, will quit.", bundle.agentInput.playerIndex)
            zombie = true
        }

        val renderer = carData.renderer
        RenderUtil.drawSphere(renderer, soonestIntercept.ballSlice.space, ArenaModel.BALL_RADIUS.toDouble(), Color.YELLOW)
        // RenderUtil.drawBallPath(renderer, ballPath, soonestIntercept.time, RenderUtil.STANDARD_BALL_PATH_COLOR)
        if (!interceptModifier.isZero) {
            RenderUtil.drawImpact(renderer, soonestIntercept.space, interceptModifier.scaled(-1F), Color.CYAN)
        }


        return getThereOnTime(bundle, soonestIntercept)
    }

    private fun getThereOnTime(bundle: TacticalBundle, intercept: Intercept): AgentOutput {

        val car = bundle.agentInput.myCarData

        SteerUtil.getSensibleFlip(car, intercept.space)?.let {
            println("Front flip toward intercept", bundle.agentInput.playerIndex)
            startPlan(it, bundle)
        }?.let { return it }

        val toTarget = (intercept.space - car.position).flatten()
        val toTargetAngle = Vector2.angle(car.orientation.noseVector.flatten(), toTarget)
        val speed = car.velocity.flatten().magnitude()
        if (toTarget.magnitude() < 15 &&
                toTargetAngle > Math.PI / 2 &&
                (speed < 5 ||
                Vector2.angle(car.velocity.flatten(), toTarget) < Math.PI / 3)) {

            startPlan(SetPieces.anyDirectionFlip(car, toTarget), bundle)?.let { return it }
        }

        val timeToIntercept = Duration.between(car.time, intercept.time)
        val motionAfterStrike = intercept.accelSlice

        val maxDistance = motionAfterStrike.distance
        val distanceToIntercept = car.position.flatten().distance(intercept.space.flatten())
        val pace = maxDistance / distanceToIntercept
        val averageSpeedNeeded = distanceToIntercept / timeToIntercept.seconds
        val currentSpeed = car.velocity.magnitude()

        val agentOutput = SteerUtil.steerTowardGroundPosition(car, intercept.space.flatten(),
                detourForBoost = false, conserveBoost = car.boost <= intercept.airBoost)

        if (pace > 1.1 && currentSpeed > averageSpeedNeeded) {
            // Slow down
            agentOutput.withThrottle(Math.min(0.0, -pace + 1.5)).withBoost(false) // Hit the brakes, but keep steering!
            if (car.orientation.noseVector.dotProduct(car.velocity) < 0) {
                // car is going backwards
                agentOutput.withThrottle(0.0).withSteer(0.0)
            }
        }
        return agentOutput

    }

    override fun drawDebugInfo(graphics: Graphics2D) {

        super.drawDebugInfo(graphics)

        chosenIntercept?.let {
            graphics.color = Color(214, 136, 29)
            graphics.stroke = BasicStroke(1f)
            val (x, y) = it.space.flatten()
            val crossSize = 2
            graphics.draw(Line2D.Float(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
            graphics.draw(Line2D.Float(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
        }
    }

    private fun getSoonestIntercept(
            carData: CarData,
            ballPath: BallPath,
            fullAcceleration: DistancePlot,
            interceptModifier: Vector3): Intercept? {

        val strikeProfileFn = { ballSpaceTime: BallSlice ->
            val desiredProfile = StrikePlanner.computeStrikeProfile(ballSpaceTime.space.z)
            if (needsChallenge && desiredProfile.style == Style.CHIP) {
                FlipHitStrike()
            } else {
                desiredProfile
            }
        }

        return InterceptCalculator.getFilteredInterceptOpportunity(
                carData, ballPath, fullAcceleration, interceptModifier, { cd, spaceTime -> StrikePlanner.isVerticallyAccessible(cd, spaceTime) }, strikeProfileFn)
    }
}
