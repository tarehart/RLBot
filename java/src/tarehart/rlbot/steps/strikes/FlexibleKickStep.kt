package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AirTouchPlanner
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.*
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.routing.*
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

import java.awt.*
import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

class FlexibleKickStep(private val kickStrategy: KickStrategy) : NestedPlanStep() {
    private lateinit var originalTouch: Optional<BallTouch>
    private var doneMoment: GameTime? = null
    //private lateinit var interceptModifier: Vector3
    //private var approachToLaunchpadError: Double = 0.0
    private var recentPrecisionPlan: PrecisionPlan? = null
    //private var recentKickPlan: DirectedKickPlan? = null
    private var recentCar: CarData? = null
    //private var earliestIntercept: GameTime? = null
    private val disruptionMeter = BallPathDisruptionMeter()

    override fun doInitialComputation(input: AgentInput) {
        recentCar = input.myCarData

        if (doneMoment == null && input.myCarData.position.distance(input.ballPosition) < 4.5) {
            // You get a tiny bit more time
            doneMoment = input.time.plus(Duration.ofMillis(200))
        }
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData

        if (!::originalTouch.isInitialized) {
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

        val situation = TacticsTelemetry[car.playerIndex] ?: return null

        //val ballAtIntercept = recentPrecisionPlan?.kickPlan?.intercept?.ballSlice?.space ?: situation.expectedContact?.ballSlice?.space ?: input.ballPosition
        //val kickDirection = kickStrategy.getKickDirection(input.myCarData, ballAtIntercept) ?: return null

        //val launchPadEstimate = recentPrecisionPlan?.kickPlan?.launchPad?.position ?: ballAtIntercept.flatten()
        //val toLaunchPadEstimate = launchPadEstimate - car.position.flatten()

        //val approachAngle = Vector2.angle(toLaunchPadEstimate, kickDirection.flatten())
        //val kickPlan: DirectedKickPlan?

        val strikeProfileFn = { height:Double, approachAngle: Double -> AirTouchPlanner.getStrikeProfile(height, approachAngle) }
        val ballPath = ArenaModel.predictBallPath(input)

        val overallPredicate = { cd: CarData, st: SpaceTime, str: StrikeProfile ->
            val verticallyAccessible = str.verticallyAccessible.invoke(cd, st)
            val viableKick = kickStrategy.looksViable(cd, st.space)
            verticallyAccessible && viableKick
        }

        val precisionPlan = InterceptCalculator.getRouteAwareIntercept(
                carData = car,
                ballPath = ballPath,
                acceleration = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(6.0), car.boost, 0.0),
                spatialPredicate = overallPredicate,
                strikeProfileFn = strikeProfileFn,
                kickStrategy = kickStrategy) ?: return null

        recentPrecisionPlan = precisionPlan

//        if (::interceptModifier.isInitialized) {
//            kickPlan = DirectedKickUtil.planKick(
//                    car,
//                    ballPath,
//                    kickStrategy,
//                    interceptModifier,
//                    strikeProfileFn,
//                    earliestIntercept ?: input.time)
//        } else {
//            kickPlan = DirectedKickUtil.planKick(car, ballPath, kickStrategy, strikeProfileFn)
//        }
//
//        if (kickPlan == null) {
//            BotLog.println("Quitting flexible hit due to failed kick plan.", car.playerIndex)
//            return null
//        }
//
//        recentKickPlan = kickPlan

        if (disruptionMeter.isDisrupted(precisionPlan.kickPlan.ballPath)) {
            BotLog.println("Ball path disrupted during flexible.", car.playerIndex)
            return null
        }


        val durationTillLaunchpad = Duration.between(car.time, precisionPlan.kickPlan.launchPad.gameTime)
        if (durationTillLaunchpad.millis < 500 && car.position.flatten().distance(precisionPlan.kickPlan.launchPad.position) < 3) {
            StrikePlanner.planImmediateLaunch(car, precisionPlan.kickPlan.intercept)?.let {
                return startPlan(it, input)
            }
        }


        if (input.latestBallTouch.map { it.position }.orElse(Vector3()) != originalTouch.map { it.position }.orElse(Vector3())) {
            // There has been a new ball touch.
            println("Ball has been touched, quitting flexible hit", input.playerIndex)
            return null
        }

        val badCirclePart = precisionPlan.steerPlan.route.parts.firstOrNull {
            part -> if (part is CircleRoutePart)
                Math.abs(part.sweepRadians) > Math.PI / 2
            else false
        }

        if (badCirclePart != null) {
            BotLog.println("Bad circle part during flexible.", car.playerIndex)
            return null
        }

//        val orientationCorrectionForStrike = car.orientation.noseVector.flatten().correctionAngle(kickPlan.launchPad.facing)
//
//        if (!::interceptModifier.isInitialized) {
//            interceptModifier = kickPlan.plannedKickForce.scaledToMagnitude(-1.4)
//        }
//
//        val timeAfterRoute = recentCircleTurnPlan?.route?.duration?.let { car.time.plus(it) }
//        val earliestThisTime = timeAfterRoute?.let {
//            if (it > kickPlan.intercept.time) it else kickPlan.intercept.time
//        } ?: kickPlan.intercept.time
//        val earliestPossibleIntercept = earliestIntercept ?: input.time
//        val timeMismatch = Duration.between(earliestPossibleIntercept, earliestThisTime).seconds
//
//
//
//        // If you're facing the intercept, but the circle backoff wants you to backtrack, you should just wait
//        // for a later intercept instead.
//        val waitForLaterIntercept = kickPlan.intercept.space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD &&
//                Math.abs(approachToLaunchpadError) > Math.PI / 2 && Math.abs(orientationCorrectionForStrike) < Math.PI / 2
//
//        if (waitForLaterIntercept) {
//            earliestIntercept = earliestPossibleIntercept.plusSeconds(.1)
//        } else if (Math.abs(approachToLaunchpadError) > Math.PI / 2) {
//            return null // Too much turning.
//        } else if (kickPlan.intercept.space.z < AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
//            earliestIntercept = earliestPossibleIntercept.plusSeconds(timeMismatch / 2)
//        }
//
//
//        val circleTurnPlan: SteerPlan
//        if (durationTillLaunchpad.seconds > .5) {
//            circleTurnPlan = CircleTurnUtil.getPlanForCircleTurn(input, kickPlan.distancePlot, kickPlan.launchPad)
//        } else {
//            // During the last moments, stop worrying about orientation
//            val travelTime = kickPlan.distancePlot.getTravelTime(kickPlan.launchPad.position.distance(car.position.flatten())) ?: return null
//            val immediateSteer =
//                    if (kickPlan.intercept.spareTime.millis <= 0)
//                        SteerUtil.steerTowardGroundPosition(car, kickPlan.launchPad.position)
//                    else
//                        SteerUtil.getThereOnTime(car, SpaceTime(kickPlan.launchPad.position.toVector3(), kickPlan.launchPad.gameTime))
//
//            circleTurnPlan = SteerPlan(immediateSteer,
//                    Route().withPart(AccelerationRoutePart(car.position.flatten(), kickPlan.launchPad.position, travelTime)))
//        }
//        circleTurnPlan.route.withPart(StrikeRoutePart(kickPlan.launchPad.position, kickPlan.intercept.space, kickPlan.intercept.strikeProfile))
//
//        recentCircleTurnPlan = circleTurnPlan

        if (ArenaModel.getDistanceFromWall(Vector3(precisionPlan.steerPlan.waypoint.x, precisionPlan.steerPlan.waypoint.y, 0.0)) < -1) {
            println("Failing flexible hit because waypoint is out of bounds", input.playerIndex)
            return null
        }


        return getNavigation(input, precisionPlan.steerPlan)
    }

    private fun getNavigation(input: AgentInput, circleTurnOption: SteerPlan): AgentOutput? {
        val car = input.myCarData

        SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint)?.let {
            println("Front flip toward flexible hit", input.playerIndex)
            return startPlan(it, input)
        }

        return circleTurnOption.immediateSteer
    }

    override fun getLocalSituation(): String {
        return "Flexible hit - " + kickStrategy.javaClass.simpleName + recentPrecisionPlan?.kickPlan?.let { " - " + it.intercept.strikeProfile.style }
    }

    override fun drawDebugInfo(graphics: Graphics2D) {

        super.drawDebugInfo(graphics)

        recentPrecisionPlan?.let {
            graphics.color = Color(138, 164, 200)
            it.steerPlan.drawDebugInfo(graphics, recentCar!!)
            it.kickPlan.drawDebugInfo(graphics)
        }
    }

    companion object {
        const val MAX_NOSE_HIT_ANGLE = Math.PI / 18

        /**
         * First you drive from where you are zero to the launchpad.
         * Then you have to
         */
        private fun measureApproachToLaunchPadCorrection(car: CarData, kickPlan: DirectedKickPlan): Double {
            val carToLaunchPad = kickPlan.launchPad.position.minus(car.position.flatten())
            return carToLaunchPad.correctionAngle(kickPlan.launchPad.facing)
        }
    }
}
