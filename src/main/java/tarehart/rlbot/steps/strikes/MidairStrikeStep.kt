package tarehart.rlbot.steps.strikes

import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.intercept.AerialMath
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
import tarehart.rlbot.planning.cancellation.InterceptDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.GameModeSniffer
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.ui.ArenaDisplay
import java.awt.Color
import java.awt.Graphics2D

class MidairStrikeStep(private val timeInAirAtStart: Duration,
                       private val offsetFn: (Vector3?, Team) -> Vector3 = { intercept, team -> standardOffset(intercept, team) },
                       private val hasJump: Boolean = true) : NestedPlanStep() {

    private lateinit var lastMomentForDodge: GameTime
    private lateinit var beginningOfStep: GameTime
    private var intercept: SpaceTime? = null

    override fun getLocalSituation(): String {
        return "Midair Strike"
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        return bundle.agentInput.myCarData.hasWheelContact
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
        val timeSinceLaunch = Duration.between(beginningOfStep, bundle.agentInput.time)

        val ballPath = bundle.tacticalSituation.ballPath
        val car = bundle.agentInput.myCarData
        val offset = offsetFn(intercept?.space, car.team)

        val latestIntercept = InterceptCalculator.getAerialIntercept(car, ballPath, offset, beginningOfStep)
        if (latestIntercept == null) {
            if (!car.hasWheelContact && car.position.z > 1) {
                // Orient toward the ball and hope for an aerial intercept to resume
                return OrientationSolver
                        .orientCar(bundle.agentInput.myCarData, Mat3.lookingTo(bundle.agentInput.ballPosition - car.position), 1.0 / 60)
                        .withThrottle(1.0)
            }
            return null
        }

        val renderer = BotLoopRenderer.forBotLoop(bundle.agentInput.bot)
        RenderUtil.drawImpact(renderer, latestIntercept.space, offset.scaled(-3.0), Color.CYAN)
        RenderUtil.drawBallPath(renderer, ballPath, latestIntercept.time, RenderUtil.STANDARD_BALL_PATH_COLOR)

        intercept = latestIntercept.toSpaceTime()
        val carToIntercept = latestIntercept.space.minus(car.position)
        val millisTillIntercept = Duration.between(bundle.agentInput.time, latestIntercept.time).millis
        val distance = car.position.distance(bundle.agentInput.ballPosition)

        val correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, latestIntercept.space)

        if (hasJump && bundle.agentInput.time.isBefore(lastMomentForDodge) && distance < DODGE_TIME.seconds * car.velocity.magnitude()
                && latestIntercept.space.z - car.position.z < 1.5) {
            // Let's flip into the ball!
            if (Math.abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
                BotLog.println("Front flip strike", bundle.agentInput.playerIndex)
                startPlan(Plan()
                        .withStep(BlindStep(Duration.ofMillis(30), AgentOutput()))
                        .withStep(BlindStep(Duration.ofSeconds(1.0), AgentOutput().withPitch(-1.0).withJump())),
                        bundle)
            } else {
                // Dodge to the side
                BotLog.println("Side flip strike", bundle.agentInput.playerIndex)
                startPlan(Plan()
                        .withStep(BlindStep(Duration.ofMillis(30), AgentOutput()))
                        .withStep(BlindStep(Duration.ofMillis(30), AgentOutput().withYaw((if (correctionAngleRad < 0) 1 else -1).toDouble()).withJump())),
                        bundle)
            }
        }

        val secondsSoFar = Duration.between(beginningOfStep, bundle.agentInput.time).seconds

        if (millisTillIntercept > DODGE_TIME.millis && secondsSoFar > 1 && Vector2.angle(car.velocity.flatten(), carToIntercept.flatten()) > Math.PI / 6) {
            BotLog.println("Failed aerial on bad angle", bundle.agentInput.playerIndex)
            return null
        }

        val projectedHeight = AerialMath.getProjectedHeight(
                car, Duration.between(car.time, latestIntercept.time).seconds,
                timeSinceLaunch.seconds
        )

        val heightError = projectedHeight - latestIntercept.space.z

        val flatToIntercept = carToIntercept.flatten()

        val currentFlatVelocity = car.velocity.flatten()

        val leftRightCorrectionAngle = currentFlatVelocity.correctionAngle(flatToIntercept)
        val desiredFlatOrientation = VectorUtil
                .rotateVector(currentFlatVelocity, leftRightCorrectionAngle * YAW_OVERCORRECT)
                .normalized()

        val desiredNoseVector: Vector3

        // Fly toward intercept
        val desiredZ = AerialMath.getDesiredZComponentBasedOnAccel(
                latestIntercept.space.z,
                Duration.between(car.time, latestIntercept.time),
                timeSinceLaunch,
                car)
        desiredNoseVector = convertToVector3WithPitch(desiredFlatOrientation, desiredZ)

        val up = if (car.orientation.roofVector.z > 0.8)
            Vector3.UP
        else
            car.orientation.roofVector

        return OrientationSolver.orientCar(car, Mat3.lookingTo(desiredNoseVector, up), 1/60.0)
                .withBoost(desiredNoseVector.dotProduct(car.orientation.noseVector) > .5)
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
        private val SIDE_DODGE_THRESHOLD = Math.PI / 4
        private val DODGE_TIME = Duration.ofMillis(400)
        //private static final double DODGE_DISTANCE = 6;
        val MAX_TIME_FOR_AIR_DODGE = Duration.ofSeconds(1.3)
        private val YAW_OVERCORRECT = 3.0
        private val NOSE_FINESSE_TIME = Duration.ofMillis(800)

        /**
         * Return a unit vector with the given z component, and the same flat angle as flatDirection.
         */
        private fun convertToVector3WithPitch(flat: Vector2, zComponent: Double): Vector3 {
            val xyScaler = Math.sqrt((1 - zComponent * zComponent) / (flat.x * flat.x + flat.y * flat.y))
            return Vector3(flat.x * xyScaler, flat.y * xyScaler, zComponent)
        }

        private fun standardOffset(intercept: Vector3?, team:Team): Vector3 {
            val ownGoal = GoalUtil.getOwnGoal(team).center
            var offset = ownGoal.scaledToMagnitude(2.0).minus(Vector3(0.0, 0.0, .3))

            intercept?.let {

                val enemyGoal = GoalUtil.getEnemyGoal(team)
                val goalToBall = it.minus(enemyGoal.getNearestEntrance(it, 4.0))

                offset = goalToBall.scaledToMagnitude(3.4)

                val gameMode = GameModeSniffer.getGameMode()
                if (gameMode == GameMode.SOCCER) {
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
