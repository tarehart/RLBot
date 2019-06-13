package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AerialMath
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.math.Mat3
import tarehart.rlbot.math.OrientationSolver
import tarehart.rlbot.math.SpaceTime
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
                       private val kickStrategy: KickStrategy? = null) : NestedPlanStep() {

    private lateinit var lastMomentForDodge: GameTime
    private lateinit var beginningOfStep: GameTime
    private var intercept: Intercept? = null
    private var lostWheelContact = false

    private var boostCounter = 0.0
    private var wasBoosting = false

    private val ballPathDisruptionMeter = BallPathDisruptionMeter(0.2)

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

        if (! ::lastMomentForDodge.isInitialized) {
            lastMomentForDodge = bundle.agentInput.time.plus(MAX_TIME_FOR_AIR_DODGE).minus(timeInAirAtStart)
            beginningOfStep = bundle.agentInput.time
        }

        // We hold down the jump button during the aerial for extra upward acceleration, but it wears off.
        val secondsSinceLaunch = Duration.between(beginningOfStep, bundle.agentInput.time).seconds

        val ballPath = bundle.tacticalSituation.ballPath
        val car = bundle.agentInput.myCarData

        val offset = standardOffset(intercept?.space, car.team, bundle)

        lostWheelContact = lostWheelContact || !car.hasWheelContact

        if (ballPathDisruptionMeter.isDisrupted(bundle.tacticalSituation.ballPath)) {
            intercept = null
            ballPathDisruptionMeter.reset()
        }

        val spatialPredicate = { cd: CarData, st: SpaceTime ->
            kickStrategy?.looksViable(cd, st.space) ?: true
        }

        val latestIntercept = InterceptCalculator.getAerialIntercept(car, ballPath, offset, beginningOfStep, spatialPredicate)
                ?: return null

        intercept = latestIntercept

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

        if (hasJump && car.time.isBefore(lastMomentForDodge) && carToIntercept.magnitude() < DODGE_TIME.seconds * car.velocity.flatten().magnitude()
                && latestIntercept.space.z - car.position.z < 2.0) {
            // Let's flip into the ball!
            if (Math.abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
                BotLog.println("Front flip strike", bundle.agentInput.playerIndex)
                startPlan(Plan()
                        .withStep(BlindStep(Duration.ofMillis(30), AgentOutput()))
                        .withStep(BlindStep(Duration.ofSeconds(1.0), AgentOutput().withPitch(-1.0).withJump())),
                        bundle)
            }
        }

        val secondsSoFar = Duration.between(beginningOfStep, bundle.agentInput.time).seconds



        val courseResult = if (canDodge && carToIntercept.z / carToIntercept.flatten().magnitude() < 0.2)
            AerialMath.calculateAerialCourseCorrection(
                    CarSlice(car),
                    SpaceTime(latestIntercept.space - carToIntercept.flatten().scaledToMagnitude(2.0).toVector3(), latestIntercept.time - DODGE_TIME),
                    modelJump = false,
                    secondsSinceJump = secondsSinceLaunch,
                    assumeResidualBoostAccel = wasBoosting)
        else
            AerialMath.calculateAerialCourseCorrection(
                    CarSlice(car),
                    latestIntercept.toSpaceTime(),
                    false,
                    secondsSinceLaunch,
                    wasBoosting)

        if (millisTillIntercept > DODGE_TIME.millis && secondsSoFar > 1 &&
                Vector2.angle(car.velocity.flatten(), carToIntercept.flatten()) > Math.PI / 6 &&
                courseResult.averageAccelerationRequired > AerialMath.BOOST_ACCEL_IN_AIR) {
            BotLog.println("Failed aerial on bad angle", bundle.agentInput.playerIndex)
            return null
        }

        if (car.boost == 0.0 && courseResult.targetError.magnitude() > 5) {
            return null
        }

        RenderUtil.drawSphere(car.renderer, latestIntercept.space - courseResult.targetError, 1.0, Color.RED)
        RenderUtil.drawSphere(car.renderer, latestIntercept.ballSlice.space, ArenaModel.BALL_RADIUS.toDouble(), Color.YELLOW)

        var useBoost = 0L
        val boostThreshold = if (secondsSinceLaunch < 1) .8  else .9
        if (courseResult.correctionDirection.dotProduct(car.orientation.noseVector) > boostThreshold) {
            useBoost -= Math.round(boostCounter)
            boostCounter += Clamper.clamp(1.25 * (courseResult.averageAccelerationRequired / AerialMath.BOOST_ACCEL_IN_AIR), 0.0, 1.0)
            useBoost += Math.round(boostCounter)
        }

        if (courseResult.targetError.magnitude() < 1) {
            return OrientationSolver.orientCar(car, Mat3.lookingTo(offset * -1.0, car.orientation.roofVector), ORIENT_DT)
                    .withJump()
        }

        wasBoosting = useBoost != 0L

        return OrientationSolver.orientCar(car, Mat3.lookingTo(courseResult.correctionDirection, car.orientation.roofVector), ORIENT_DT)
                .withBoost(useBoost != 0L)
                .withJump()

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
        private const val ORIENT_DT = 1/60.0

        private fun standardOffset(intercept: Vector3?, team: Team, tacticalBundle: TacticalBundle): Vector3 {
            val ownGoal = GoalUtil.getOwnGoal(team).center
            var offset = ownGoal.scaledToMagnitude(2.0).minus(Vector3(0.0, 0.0, .3))

            intercept?.let {

                val enemyGoal = GoalUtil.getEnemyGoal(team)
                val goalToBall = it.minus(enemyGoal.getNearestEntrance(it, 4.0))

                offset = goalToBall.scaledToMagnitude(3.37)

                val gameMode = GameModeSniffer.getGameMode()
                if (gameMode == GameMode.SOCCER) {

                    if (tacticalBundle.tacticalSituation.scoredOnThreat != null) {
                        offset = offset.scaledToMagnitude(3.0)
                    }

                    if (Math.abs(ownGoal.y - intercept.y) > ArenaModel.BACK_WALL * .7 && goalToBall.magnitude() > 110) {
                        offset = Vector3(offset.x, offset.y, -.2)
                    }
                } else if (gameMode == GameMode.HOOPS) {
                    if (enemyGoal.center.flatten().distance(intercept.flatten()) < HoopsGoal.RADIUS + intercept.z - HoopsGoal.GOAL_HEIGHT ) {
                        // Dunk it!
                        offset = Vector3(offset.x, offset.y, 0.5)
                    } else {
                        // Loft it
                        offset = Vector3(offset.x, offset.y, -0.7)
                    }
                } else {
                    if (enemyGoal.center.y * intercept.y > 0 ) {
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
