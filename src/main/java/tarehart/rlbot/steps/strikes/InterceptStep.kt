package tarehart.rlbot.steps.strikes

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.strike.AerialStrike
import tarehart.rlbot.intercept.strike.FlipHitStrike
import tarehart.rlbot.intercept.strike.JumpHitStrike
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.util.*

class InterceptStep(
        private val interceptModifier: Vector3,
        private val interceptPredicate: (CarData, SpaceTime) -> Boolean = { _, _ -> true }
) : NestedPlanStep() {
    override fun getLocalSituation(): String {
        return  "Working on intercept"
    }

    private var originalIntercept: Intercept? = null
    private var chosenIntercept: Intercept? = null
    private var originalTouch: BallTouch? = null

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val carData = bundle.agentInput.myCarData
        val ballPath = bundle.tacticalSituation.ballPath
        val fullAcceleration = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(7.0), carData.boost, 0.0)

        val soonestIntercept = getSoonestIntercept(carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate)
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

        if (originalIntercept == null) {
            originalIntercept = soonestIntercept
            originalTouch = bundle.agentInput.latestBallTouch

        } else {

            if (originalIntercept?.let { ballPath.getMotionAt(it.time)?.space?.distance(it.space)?.takeIf { it > 10 } } != null) {
                println("Ball slices has diverged from expectation, will quit.", bundle.agentInput.playerIndex)
                zombie = true
            }

            if (originalTouch?.position ?: Vector3() != bundle.agentInput.latestBallTouch?.position ?: Vector3()) {
                // There has been a new ball touch.
                println("Ball has been touched, quitting intercept", bundle.agentInput.playerIndex)
                return null
            }
        }

        val renderer = BotLoopRenderer.forBotLoop(bundle.agentInput.bot)
        RenderUtil.drawSphere(renderer, soonestIntercept.ballSlice.space, ArenaModel.BALL_RADIUS.toDouble(), Color.YELLOW)
        RenderUtil.drawBallPath(renderer, ballPath, soonestIntercept.time, RenderUtil.STANDARD_BALL_PATH_COLOR)
        if (!interceptModifier.isZero) {
            RenderUtil.drawImpact(renderer, soonestIntercept.space, interceptModifier.scaled(-1.0), Color.CYAN)
        }


        return getThereOnTime(bundle, soonestIntercept)
    }

    private fun getThereOnTime(bundle: TacticalBundle, intercept: Intercept): AgentOutput {

        val car = bundle.agentInput.myCarData

        SteerUtil.getSensibleFlip(car, intercept.space)?.let {
            println("Front flip toward intercept", bundle.agentInput.playerIndex)
            startPlan(it, bundle)
        }?.let { return it }

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
            graphics.draw(Line2D.Double(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
            graphics.draw(Line2D.Double(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
        }
    }

    companion object {

        fun getSoonestIntercept(
                carData: CarData,
                ballPath: BallPath,
                fullAcceleration: DistancePlot,
                interceptModifier: Vector3,
                interceptPredicate: (CarData, SpaceTime) -> Boolean): Intercept? {

            val interceptOptions = ArrayList<Intercept>()

            getAerialIntercept(carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate)?.let { if (it.space.z >= StrikePlanner.NEEDS_AERIAL_THRESHOLD) interceptOptions.add(it) }
            getJumpHitIntercept(carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate)?.let { interceptOptions.add(it) }
            getFlipHitIntercept(carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate)?.let { interceptOptions.add(it) }

            return interceptOptions.asSequence().sortedBy { intercept -> intercept.time }.firstOrNull()
        }

        private fun getAerialIntercept(carData: CarData, ballPath: BallPath, fullAcceleration: DistancePlot, interceptModifier: Vector3, interceptPredicate: (CarData, SpaceTime) -> Boolean): Intercept? {
            if (carData.boost < StrikePlanner.BOOST_NEEDED_FOR_AERIAL) return null

            return InterceptCalculator.getFilteredInterceptOpportunity(carData, ballPath, fullAcceleration, interceptModifier,
                    { cd, st -> interceptPredicate.invoke(cd, st) && StrikePlanner.isVerticallyAccessible(cd, st) },
                    { height -> AerialStrike(height) })
        }

        private fun getJumpHitIntercept(carData: CarData, ballPath: BallPath, fullAcceleration: DistancePlot, interceptModifier: Vector3, interceptPredicate: (CarData, SpaceTime) -> Boolean): Intercept? {
            return InterceptCalculator.getFilteredInterceptOpportunity(
                    carData, ballPath, fullAcceleration, interceptModifier,
                    { cd, st -> interceptPredicate.invoke(cd, st) && st.space.z < JumpHitStrike.MAX_BALL_HEIGHT_FOR_JUMP_HIT },
                    { height: Double -> JumpHitStrike(height) })
        }

        private fun getFlipHitIntercept(carData: CarData, ballPath: BallPath, fullAcceleration: DistancePlot, interceptModifier: Vector3, interceptPredicate: (CarData, SpaceTime) -> Boolean): Intercept? {
            return InterceptCalculator.getFilteredInterceptOpportunity(
                    carData, ballPath, fullAcceleration, interceptModifier,
                    { cd, st -> interceptPredicate.invoke(cd, st) && FlipHitStrike.isVerticallyAccessible(st.space.z) },
                    { FlipHitStrike() })
        }
    }
}
