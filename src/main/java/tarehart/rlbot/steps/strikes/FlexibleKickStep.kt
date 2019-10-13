package tarehart.rlbot.steps.strikes

import rlbot.render.NamedRenderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.Zone
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.routing.CircleRoutePart
import tarehart.rlbot.routing.PrecisionPlan
import tarehart.rlbot.routing.SteerPlan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.BotLog.println
import java.awt.Color
import java.awt.Graphics2D

class FlexibleKickStep(private val kickStrategy: KickStrategy) : NestedPlanStep() {
    private var doneMoment: GameTime? = null
    private var initialized = false
    //private lateinit var interceptModifier: Vector3
    //private var approachToLaunchpadError: Double = 0.0
    private var recentPrecisionPlan: PrecisionPlan? = null
    //private var recentKickPlan: DirectedKickPlan? = null
    private var recentCar: CarData? = null
    //private var earliestIntercept: GameTime? = null
    private val disruptionMeter = BallPathDisruptionMeter()
    private var strikeHint: StrikeProfile.Style? = null

    override fun doInitialComputation(bundle: TacticalBundle) {
        recentCar = bundle.agentInput.myCarData

        if (doneMoment == null && bundle.agentInput.myCarData.position.distance(bundle.agentInput.ballPosition) < 4.5) {
            // You get a tiny bit more time
            doneMoment = bundle.agentInput.time.plus(Duration.ofMillis(200))
        }
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        if (!initialized) {
            initialized = true
        }

        doneMoment?.let {
            if (bundle.agentInput.time.isAfter(it)) {
                return null
            }
        }

        if (ArenaModel.isCarOnWall(car)) {
            return null
        }

        val strikeProfileFn = {
            intercept: Vector3, kickDirection: Vector2, c: CarData ->
                val approachVec = intercept.flatten() - c.position.flatten()
                getStrikeProfile(intercept, Vector2.angle(approachVec, kickDirection), kickStrategy, strikeHint, car)

        }
        val ballPath = bundle.tacticalSituation.ballPath

        val overallPredicate = { cd: CarData, st: SpaceTime, str: StrikeProfile ->
            val verticallyAccessible = str.isVerticallyAccessible(cd, st)
            val viableKick = kickStrategy.looksViable(cd, st.space)
            verticallyAccessible && viableKick
        }

        // TODO: This is way too pessimistic for aerials at the moment. Possibly the aerial strike profile is being too
        // conservative about how long it takes to get up for a ball, since I hacked the aerial rise rate from
        // 10 to 5 to make it take off faster. Probably need to undo that hack and find a better way.
        val precisionPlan = InterceptCalculator.getRouteAwareIntercept(
                carData = car,
                ballPath = ballPath,
                spatialPredicate = overallPredicate,
                strikeProfileFn = strikeProfileFn,
                kickStrategy = kickStrategy) ?: return null

        if ((kickStrategy.isShotOnGoal() && bundle.tacticalSituation.teamPlayerWithBestShot?.car != car ||
                        !kickStrategy.isShotOnGoal() && bundle.tacticalSituation.teamPlayerWithInitiative?.car != car)  &&
                (precisionPlan.kickPlan.intercept.time - car.time).seconds > 1.0) {
            return null // Give up on the shot
        }

        recentPrecisionPlan = precisionPlan

        if (kickStrategy is KickAtEnemyGoal &&
                !precisionPlan.kickPlan.intercept.strikeProfile.isForward &&
                Zone.isInDefensiveThird(bundle.zonePlan.ballZone, car.team)) {
            // Don't do long-range diagonal or side dodges, it leaves us open to counter attacks.
            return null
        }

        if (strikeHint == null) {
            val steerCorrection = SteerUtil.getCorrectionAngleRad(car, precisionPlan.steerPlan.waypoint)
            if (Math.abs(steerCorrection) < Math.PI / 20) {
                strikeHint = precisionPlan.kickPlan.intercept.strikeProfile.style
            }
        }

        if (disruptionMeter.isDisrupted(precisionPlan.kickPlan.ballPath)) {
            BotLog.println("Ball path disrupted during flexible.", car.playerIndex)
            cancelPlan = true
            return null
        }

        if (FinalApproachStep.readyForFinalApproach(car, precisionPlan.kickPlan.launchPad)) {
            return startPlan(Plan().withStep(FinalApproachStep(precisionPlan.kickPlan)), bundle)
        }

        val badCirclePart = precisionPlan.steerPlan.route.parts.firstOrNull {
            part -> if (part is CircleRoutePart)
                Math.abs(part.sweepRadians) > Math.PI / 3 || part.circle.radius < 20
            else false
        }

        if (badCirclePart != null) {
//            val renderer = NamedRenderer("badRoute")
//            renderer.startPacket()
//            precisionPlan.steerPlan.route.renderDebugInfo(renderer)
//            renderer.finishAndSend()
            BotLog.println("Bad circle part during flexible.", car.playerIndex)
            return null
        }

        if (ArenaModel.getDistanceFromWall(Vector3(precisionPlan.steerPlan.waypoint.x, precisionPlan.steerPlan.waypoint.y, 0.0)) < -1) {
            println("Failing flexible hit because waypoint is out of bounds", bundle.agentInput.playerIndex)
            return null
        }

        val renderer = car.renderer
        precisionPlan.kickPlan.renderDebugInfo(renderer)
        precisionPlan.steerPlan.route.renderDebugInfo(renderer)

        return getNavigation(bundle, precisionPlan.steerPlan)
    }

    private fun getNavigation(bundle: TacticalBundle, circleTurnOption: SteerPlan): AgentOutput? {
        val car = bundle.agentInput.myCarData

        if (car.boost < 1) {
            SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint)?.let {
                println("Front flip toward flexible hit", bundle.agentInput.playerIndex)
                return startPlan(it, bundle)
            }
        }

        return circleTurnOption.immediateSteer
    }

    override fun getLocalSituation(): String {
        return "Flexible hit - " + kickStrategy.javaClass.simpleName +
                recentPrecisionPlan?.kickPlan?.let {
                    " - " + it.intercept.strikeProfile.style
                }
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
        fun getStrikeProfile(intercept: Vector3, approachAngleMagnitude: Double, kickStrategy: KickStrategy,
                             styleHint: StrikeProfile.Style?, car: CarData): StrikeProfile {

            if (styleHint == StrikeProfile.Style.SIDE_HIT || styleHint == StrikeProfile.Style.DIAGONAL_HIT) {
                // If we were going for a particular sideways hit, stay committed to it. Sometimes the
                // angle changes as a natural part of the leadup and we don't want to thrash based on that.
                return StrikePlanner.getStrikeProfile(styleHint, intercept.z, kickStrategy)
            }
            val style = StrikePlanner.computeStrikeStyle(car, intercept, approachAngleMagnitude)
            return StrikePlanner.getStrikeProfile(style, intercept.z, kickStrategy)
        }
    }
}
