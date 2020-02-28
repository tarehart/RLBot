package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AerialMath
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.math.Mat3
import tarehart.rlbot.math.OrientationSolver
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.HoopsGoal
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.GameModeSniffer
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.ui.ArenaDisplay
import java.awt.Color
import java.awt.Graphics2D

class MidairStrikeStep(private val timeInAirAtStart: Duration,
                       private val hasJump: Boolean = true,
                       private val kickStrategy: KickStrategy? = null,
                       initialIntercept: SpaceTime? = null) : NestedPlanStep() {

    private lateinit var lastMomentForDodge: GameTime
    private lateinit var beginningOfStep: GameTime
    private var initialTouch: BallTouch? = null
    private var intercept: Intercept? = null
    private var lostWheelContact = false

    private var wasBoosting = false
    private var finalOrientation = false

    private val ballPathDisruptionMeter = BallPathDisruptionMeter(1.0)
    // private var spaceTime = initialIntercept

    override fun getLocalSituation(): String {
        return "Midair Strike"
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        if (lostWheelContact && bundle.agentInput.myCarData.hasWheelContact) {
            BotLog.println("Aborting midair strike due to wheel contact!", bundle.agentInput.playerIndex)
            return true
        }
        return false
    }

    override fun canAbortPlanInternally(): Boolean {
        return true
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val ballPath = bundle.tacticalSituation.ballPath
        val car = bundle.agentInput.myCarData

        val offset = standardOffset(intercept, car.team, bundle)

        val spatialPredicate = { cd: CarData, st: SpaceTime ->
            kickStrategy?.looksViable(cd, st.space) ?: true
        }

        if (! ::lastMomentForDodge.isInitialized) {
            lastMomentForDodge = bundle.agentInput.time.plus(MAX_TIME_FOR_AIR_DODGE).minus(timeInAirAtStart)
            beginningOfStep = bundle.agentInput.time
            initialTouch = bundle.agentInput.latestBallTouch
        }

        bundle.agentInput.latestBallTouch?.let { latest ->
            if (initialTouch == null) {
                return null
            }
            initialTouch?.let {
                if (latest.time != it.time) {
                    return null
                }
            }
        }

        // We hold down the jump button during the aerial for extra upward acceleration, but it wears off.
        val secondsSinceLaunch = Duration.between(beginningOfStep, bundle.agentInput.time).seconds

        lostWheelContact = lostWheelContact || !car.hasWheelContact

        if (ballPathDisruptionMeter.isDisrupted(bundle.tacticalSituation.ballPath)) {
            BotLog.println("Ball path disrupted!", car.playerIndex)
            finalOrientation = false
            ballPathDisruptionMeter.reset()
        }

        if ((car.time - beginningOfStep).seconds < 2)
            intercept = InterceptCalculator.getAerialIntercept(car, ballPath, offset, beginningOfStep, spatialPredicate)

        val latestIntercept = intercept?.toSpaceTime() ?: return null
        // val latestIntercept = spaceTime ?: return null


        if (latestIntercept.time < car.time) {
            return null // We missed the intercept
        }

        val renderer = car.renderer
        RenderUtil.drawImpact(renderer, latestIntercept.space, offset.scaled(-3.0), Color.CYAN)
        RenderUtil.drawBallPath(renderer, ballPath, latestIntercept.time, RenderUtil.STANDARD_BALL_PATH_COLOR)

        val canDodge = latestIntercept.time < lastMomentForDodge
        val carToIntercept = latestIntercept.space.minus(car.position)
        val millisTillIntercept = Duration.between(bundle.agentInput.time, latestIntercept.time).millis

        // TODO: might want to make this velocity-based
        val correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, latestIntercept.space)

//        if (hasJump && car.time.isBefore(lastMomentForDodge) && carToIntercept.magnitude() < DODGE_TIME.seconds * car.velocity.flatten().magnitude()
//                && latestIntercept.space.z - car.position.z < 1.0) {
//            // Let's flip into the ball!
//            if (Math.abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
//                BotLog.println("Front flip strike", bundle.agentInput.playerIndex)
//                startPlan(Plan()
//                        .withStep(BlindStep(Duration.ofMillis(30), AgentOutput()))
//                        .withStep(BlindStep(Duration.ofSeconds(1.0), AgentOutput().withPitch(-1.0).withJump())),
//                        bundle)
//            }
//        }

        if (finalOrientation && millisTillIntercept < 500) {
            return orientForFinalTouch(offset, car).withJump()
        } else {
            finalOrientation = false
        }

        val secondsSoFar = Duration.between(beginningOfStep, bundle.agentInput.time).seconds

        val courseResult = if (canDodge && carToIntercept.z / carToIntercept.flatten().magnitude() < 0.2)
            AerialMath.calculateAerialCourseCorrection(
                    CarSlice(car),
                    SpaceTime(latestIntercept.space - carToIntercept.flatten().scaledToMagnitude(2.0).toVector3(), latestIntercept.time - DODGE_TIME),
                    modelJump = false,
                    secondsSinceJump = secondsSinceLaunch)
        else
            AerialMath.calculateAerialCourseCorrection(
                    CarSlice(car),
                    latestIntercept,
                    false,
                    secondsSinceLaunch)

        val acceptableError: Double
        if (courseResult.targetError.dotProduct(carToIntercept) < .5) {
            // If we've overshot
            acceptableError = 2.0
        } else {
            acceptableError = 0.1
        }

        if (courseResult.targetError.magnitude() < acceptableError && !canDodge) {
            finalOrientation = true
            BotLog.println("Doing final orientation for aerial touch!", car.playerIndex)
            return orientForFinalTouch(offset, car).withJump()
        }

        val up = if (offset.z > 0 && secondsSinceLaunch > 1.5) Vector3.DOWN else Vector3.UP

        if (courseResult.targetError.magnitude() < acceptableError && !canDodge && !wasBoosting) {
            return OrientationSolver.orientCar(car, Mat3.lookingTo(courseResult.correctionDirection, up), ORIENT_DT).withJump()
        }

        if (millisTillIntercept > DODGE_TIME.millis && secondsSoFar > 1 &&
                Vector2.angle(car.velocity.flatten(), carToIntercept.flatten()) > Math.PI / 6 &&
                courseResult.averageAccelerationRequired > AerialMath.BOOST_ACCEL_IN_AIR) {
            BotLog.println("Failed aerial on bad angle", bundle.agentInput.playerIndex)
            return null
        }

        if (car.boost == 0.0 && courseResult.targetError.magnitude() > 5) {
            return null
        }

        val boostThreshold = if (secondsSinceLaunch < 1) .8  else .98

        RenderUtil.drawSphere(car.renderer, latestIntercept.space - courseResult.targetError, 2.0, Color.RED)

        wasBoosting = courseResult.correctionDirection.dotProduct(car.orientation.noseVector) > boostThreshold

        return OrientationSolver.orientCar(car, Mat3.lookingTo(courseResult.correctionDirection, up), ORIENT_DT)
                .withJump()
                .withBoost(wasBoosting)

    }

    private fun orientForFinalTouch(offset: Vector3, car: CarData): AgentOutput {
        // If we're facing the wrong way, hit it with the tail!
        // val direction = offset.scaled(offset.dotProduct(car.orientation.noseVector))
        val up = if (offset.z > 0) Vector3.DOWN else Vector3.UP
        return OrientationSolver.orientCar(car, Mat3.lookingTo(offset.scaled(-1.0), up), ORIENT_DT)
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        super.drawDebugInfo(graphics)

        intercept?.let {
            ArenaDisplay.drawBall(it.space, graphics, Color(23, 194, 8))
        }
    }

    companion object {
        private const val SIDE_DODGE_THRESHOLD = Math.PI / 4
        private val DODGE_TIME = Duration.ofMillis(300)
        val MAX_TIME_FOR_AIR_DODGE = Duration.ofSeconds(1.3)
        const val ORIENT_DT = 1/60.0

        fun standardOffset(intercept: Intercept?, team: Team, tacticalBundle: TacticalBundle): Vector3 {
            val ownGoal = GoalUtil.getOwnGoal(team).center
            var offset = ownGoal.scaledToMagnitude(2.0).minus(Vector3(0.0, 0.0, .3))
            val car = tacticalBundle.agentInput.myCarData

            intercept?.let {

                val ballPos = it.ballSlice.space
                val enemyGoal = GoalUtil.getEnemyGoal(team)
                val goalToBall = it.ballSlice.space.minus(enemyGoal.getNearestEntrance(ballPos, 6.0))


                val orthogonal = VectorUtil.orthogonal(goalToBall.flatten())
                val transverseBallVelocity = VectorUtil.project(it.ballSlice.velocity.flatten(), orthogonal)

                offset = goalToBall.plus(transverseBallVelocity.withZ(0.0)).scaledToMagnitude(3.2)

                val gameMode = GameModeSniffer.getGameMode()
                if (gameMode == GameMode.SOCCER) {

                    if (tacticalBundle.tacticalSituation.scoredOnThreat != null) {
                        offset = (offset.scaledToMagnitude(0.5) +
                                KickAwayFromOwnGoal().getKickDirection(car, ballPos).scaledToMagnitude(-1.0))
                                .withZ(-1.0)
                                .scaledToMagnitude(2.8)
                    }

                    if (Math.abs(ownGoal.y - intercept.space.y) > ArenaModel.BACK_WALL * .7 && goalToBall.magnitude() > 110) {
                        offset = Vector3(offset.x, offset.y, -.2)
                    }
                } else if (gameMode == GameMode.HOOPS) {

                    val funnelAngle = if (ArenaModel.isMicroGravity()) 2.0 else 1.0
                    if (enemyGoal.center.flatten().distance(ballPos.flatten()) / funnelAngle
                            < HoopsGoal.RADIUS + ballPos.z - HoopsGoal.GOAL_HEIGHT ) {
                        // Dunk it!
                        if (ArenaModel.isMicroGravity()) {
                            offset = Vector3(offset.x, offset.y, offset.z).scaledToMagnitude(2.8)
                        } else {
                            offset = Vector3(offset.x, offset.y, 0.5)
                        }
                    } else {
                        // Loft it
                        offset = Vector3(offset.x, offset.y, -0.7)
                    }
                } else {
                    if (enemyGoal.center.y * ballPos.y > 0 ) {
                        // Dunk it!
                        offset = Vector3(offset.x, offset.y, 0.5)
                    } else {
                        // Loft it
                        offset = Vector3(offset.x, offset.y, -0.7)
                    }
                }
            }

            return offset
        }
    }
}
