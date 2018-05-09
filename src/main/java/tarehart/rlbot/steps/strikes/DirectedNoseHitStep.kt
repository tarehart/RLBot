package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AirTouchPlanner
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.*
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

import java.awt.*
import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

class DirectedNoseHitStep(private val kickStrategy: KickStrategy) : NestedPlanStep() {
    private lateinit var originalIntercept: BallSlice
    private var originalTouch: BallTouch? = null
    private var doneMoment: GameTime? = null
    private lateinit var interceptModifier: Vector3
    private var carLaunchpadInterceptAngle: Double = 0.0
    private var recentCircleTurnPlan: SteerPlan? = null
    private var recentKickPlan: DirectedKickPlan? = null
    private var recentCar: CarData? = null
    private lateinit var earliestPossibleIntercept: GameTime

    override fun doInitialComputation(input: AgentInput) {
        recentCar = input.myCarData

        if (doneMoment == null && input.myCarData.position.distance(input.ballPosition) < 4.5) {
            // You get a tiny bit more time
            doneMoment = input.time.plus(Duration.ofMillis(200))
        }
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData

        if (!::earliestPossibleIntercept.isInitialized) {
            earliestPossibleIntercept = input.time
            originalTouch = input.latestBallTouch
        }

        doneMoment?.let {
            if (input.time.isAfter(it)) {
                return null
            }
        }

        if (ArenaModel.isCarOnWall(car)) {
            return null
        }

        val kickPlan = planKick(input)

        if (kickPlan == null) {
            BotLog.println("Quitting nose hit due to failed kick plan.", car.playerIndex)
            return null
        }

        recentKickPlan = kickPlan

        if (!::originalIntercept.isInitialized) {
            originalIntercept = kickPlan.ballAtIntercept
        } else {
            if (kickPlan.ballPath.getMotionAt(originalIntercept.time)?.space?.distance(originalIntercept.space)?.takeIf { it > 20 } != null) {
                println("Ball slices has diverged from expectation, will quit.", input.playerIndex)
                zombie = true
            }
        }

        if (input.latestBallTouch?.position ?: Vector3() != originalTouch?.position ?: Vector3()) {
            // There has been a new ball touch.
            println("Ball has been touched, quitting nose hit", input.playerIndex)
            return null
        }

        val strikeForceFlat = kickPlan.plannedKickForce.flatten().normalized()
        val interceptLocation = kickPlan.intercept.space
        val carToIntercept = interceptLocation.minus(car.position).flatten()
        val interceptLocationFlat = interceptLocation.flatten()

        carLaunchpadInterceptAngle = measureCarLaunchpadInterceptAngle(car, kickPlan)
        val positionCorrectionForStrike = carToIntercept.correctionAngle(strikeForceFlat)
        val orientationCorrectionForStrike = car.orientation.noseVector.flatten().correctionAngle(strikeForceFlat)

        if (!::interceptModifier.isInitialized) {
            interceptModifier = kickPlan.plannedKickForce.scaledToMagnitude(-1.4)
        }

        if (kickPlan.easyKickAllowed || interceptLocation.z > 2 && Math.abs(positionCorrectionForStrike) < Math.PI / 12 && Math.abs(orientationCorrectionForStrike) < Math.PI / 12) {
            recentCircleTurnPlan = null
            return startPlan(Plan().withStep(InterceptStep(interceptModifier) { c, (_, time) -> !time.isBefore(earliestPossibleIntercept) }), input)
        }


        val timeAfterRoute = recentCircleTurnPlan?.route?.duration?.let { car.time.plus(it) }
        val earliestThisTime = timeAfterRoute?.let {
            if (it > kickPlan.intercept.time) it else kickPlan.intercept.time
        } ?: kickPlan.intercept.time
        val timeMismatch = Duration.between(earliestPossibleIntercept, earliestThisTime).seconds

        if (Math.abs(positionCorrectionForStrike) > Math.PI / 2 || Math.abs(carLaunchpadInterceptAngle) > Math.PI / 2) {
            return null // Too much turning.
        }

        // If you're facing the intercept, but the circle backoff wants you to backtrack, you should just wait
        // for a later intercept instead.
        val waitForLaterIntercept = Math.abs(carLaunchpadInterceptAngle) > Math.PI / 2 && Math.abs(orientationCorrectionForStrike) < Math.PI / 2

        if (waitForLaterIntercept) {
            earliestPossibleIntercept = earliestPossibleIntercept.plusSeconds(.5)
        } else if (kickPlan.intercept.space.z < AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
            earliestPossibleIntercept = earliestPossibleIntercept.plusSeconds(timeMismatch / 2)
        } else if (Math.abs(positionCorrectionForStrike) < MAX_NOSE_HIT_ANGLE) {
            val circleTurnPlan = SteerPlan(
                    SteerUtil.steerTowardGroundPosition(car, interceptLocationFlat),
                    Route(car.time)
                            .withPart(AccelerationRoutePart(
                                    car.position.flatten(),
                                    kickPlan.launchPad!!.position,
                                    Duration.between(car.time, kickPlan.launchPad.gameTime))))

            recentCircleTurnPlan = circleTurnPlan
            return getNavigation(input, circleTurnPlan)
        }

        val asapSeconds = kickPlan.distancePlot.getMotionUponArrival(car, kickPlan.ballAtIntercept.space, StrikeProfile())
                ?.time ?: Duration.between(input.time, kickPlan.ballAtIntercept.time).seconds

        //            if (secondsTillIntercept > asapSeconds + .5) {
        //                plan = new Plan(Plan.Posture.OFFENSIVE)
        //                        .withStep(new SlideToPositionStep((in) -> new PositionFacing(circleTerminus, strikeForceFlat)));
        //                return plan.getOutput(input);
        //            }

        // Line up for a nose hit
        val fullSpeed = kickPlan.intercept.spareTime.seconds < .2 && kickPlan.intercept.space.z < 2
        val launchPad = if (fullSpeed)
            StrikePoint(kickPlan.launchPad!!.position, kickPlan.launchPad.facing, GameTime(0))
        else
            kickPlan.launchPad

        val circleTurnPlan = CircleTurnUtil.getPlanForCircleTurn(input, kickPlan.distancePlot, launchPad!!)
        recentCircleTurnPlan = circleTurnPlan

        if (ArenaModel.getDistanceFromWall(Vector3(circleTurnPlan.waypoint.x, circleTurnPlan.waypoint.y, 0.0)) < -1) {
            println("Failing nose hit because waypoint is out of bounds", input.playerIndex)
            return null
        }


        return getNavigation(input, circleTurnPlan)
    }

    private fun planKick(input: AgentInput): DirectedKickPlan? {
        return if (::interceptModifier.isInitialized) {
            DirectedKickUtil.planKick(
                    input,
                    kickStrategy,
                    interceptModifier,
                    AirTouchPlanner::getStraightOnStrikeProfile,
                    earliestPossibleIntercept)
        } else {
            DirectedKickUtil.planKick(input, kickStrategy, AirTouchPlanner::getStraightOnStrikeProfile)
        }
    }

    private fun getNavigation(input: AgentInput, circleTurnOption: SteerPlan): AgentOutput? {
        val car = input.myCarData

        SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint)?.let {
            println("Front flip toward nose hit", input.playerIndex)
            return startPlan(it, input)
        }

        return circleTurnOption.immediateSteer
    }

    override fun getLocalSituation(): String {
        return "Nose hit - " + kickStrategy.javaClass.simpleName
    }

    override fun drawDebugInfo(graphics: Graphics2D) {

        super.drawDebugInfo(graphics)

        recentCircleTurnPlan?.let {
            graphics.color = Color(138, 164, 200)
            it.drawDebugInfo(graphics, recentCar!!)
        }

        recentKickPlan?.drawDebugInfo(graphics)
    }

    companion object {
        const val MAX_NOSE_HIT_ANGLE = Math.PI / 18

        /**
         * First you drive from where you are zero to the launchpad.
         * Then you have to
         */
        private fun measureCarLaunchpadInterceptAngle(car: CarData, kickPlan: DirectedKickPlan): Double {
            val strikeForceFlat = kickPlan.plannedKickForce.flatten()
            val carToLaunchPad = kickPlan.launchPad!!.position.minus(car.position.flatten())
            return carToLaunchPad.correctionAngle(strikeForceFlat)
        }
    }
}
