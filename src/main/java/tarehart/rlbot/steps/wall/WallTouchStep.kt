package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.strike.ChipStrike
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.*
import tarehart.rlbot.planning.cancellation.InterceptDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.ui.ArenaDisplay
import java.awt.Color
import java.awt.Graphics2D

class WallTouchStep : NestedPlanStep() {

    private var latestIntercept: Intercept? = null
    private var disruptionMeter = InterceptDisruptionMeter(20.0)
    private var confusion = 0

    override fun getLocalSituation(): String {
        return "Making a wall touch."
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val input = bundle.agentInput
        val car = input.myCarData
        if (!car.hasWheelContact) {
            println("Failed to make the wall touch because the car has no wheel contact", input.playerIndex)
            return null
        }
        val ballPath = bundle.tacticalSituation.ballPath
        val fullAcceleration = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost, 0.0)

        val interceptOpportunity = InterceptCalculator.getFilteredInterceptOpportunity(
                car,
                ballPath,
                fullAcceleration,
                interceptModifier = Vector3(),
                spatialPredicate = { c: CarData, ballPosition: SpaceTime -> isBallOnWall(c, ballPosition) && isAcceptableZoneForWallTouch(bundle, ballPosition.space) },
                strikeProfileFn = { ChipStrike() },
                planeNormal = car.orientation.roofVector)

        latestIntercept = interceptOpportunity

        val motion = interceptOpportunity?.ballSlice

        if (motion == null || disruptionMeter.isDisrupted(motion) || interceptOpportunity.spatialPredicateFailurePeriod > Duration.ofSeconds(0.5)) {
            confusion++
            if (confusion > 1) {
                println("Failed to make the wall touch because the intercept changed", input.playerIndex)
                return null
            }
            return AgentOutput().withThrottle(1.0)
        } else {
            confusion = 0
        }

        RenderUtil.drawSphere(car.renderer, motion.space, ArenaModel.BALL_RADIUS.toDouble(), LOVELY_TEAL)

        val plane = ArenaModel.getNearestPlane(motion.space)
        if (plane.normal.z == 1.0) {
            println("Failed to make the wall touch because the ball is now close to the ground", input.playerIndex)
            return null
        }

        val contactPoint = motion.space - plane.normal // Normally we would just use motion.space, but make it a little closer to the wall.
        if (readyToJump(bundle, SpaceTime(contactPoint, motion.time))) {
            println("Jumping for wall touch.", input.playerIndex)
            // Continue this step until it becomes quite likely that we've hit the ball. Transitioning to
            // midair strike immediately before ball contact is unpleasant.
            return startPlan(
                    Plan(Posture.NEUTRAL)
                            .withStep(BlindStep(Duration.ofSeconds(.1), AgentOutput().withThrottle(1.0).withJump()))
                            .withStep(MidairStrikeStep(Duration.ofMillis(0))),
                    bundle)
        }

        return SteerUtil.steerTowardPositionAcrossSeam(car, motion.space)
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        super.drawDebugInfo(graphics)

        latestIntercept?.let {
            ArenaDisplay.drawBall(it.space, graphics, LOVELY_TEAL)
        }
    }

    companion object {
        val ACCEPTABLE_WALL_DISTANCE = (ArenaModel.BALL_RADIUS + 2).toDouble()
        val WALL_DEPART_SPEED = 10.0
        private val MIN_HEIGHT = 6.0
        private val LOVELY_TEAL = Color(16, 194, 140)

        private fun isBallOnWall(car: CarData, ballPosition: SpaceTime): Boolean {
            return ballPosition.space.z > MIN_HEIGHT && ArenaModel.getDistanceFromWall(ballPosition.space) <= ACCEPTABLE_WALL_DISTANCE
        }

        private fun isBallOnWall(ballPosition: BallSlice): Boolean {
            return ballPosition.space.z > MIN_HEIGHT && ArenaModel.getDistanceFromWall(ballPosition.space) <= ACCEPTABLE_WALL_DISTANCE
        }

        private val NEEDS_JUMP_THRESHOLD = ArenaModel.BALL_RADIUS + .3

        private fun readyToJump(bundle: TacticalBundle, carPositionAtContact: SpaceTime): Boolean {

            val input = bundle.agentInput
            val car = input.myCarData
            if (ArenaModel.getDistanceFromWall(carPositionAtContact.space) < NEEDS_JUMP_THRESHOLD || !ArenaModel.isCarOnWall(car)) {
                return false // Really close to wall, no need to jump. Just chip it.
            }
            val toPosition = carPositionAtContact.space.minus(car.position)
            val correctionAngleRad = VectorUtil.getCorrectionAngle(car.orientation.noseVector, toPosition, car.orientation.roofVector)
            val secondsTillIntercept = Duration.between(input.time, carPositionAtContact.time).seconds
            val wallDistanceAtIntercept = ArenaModel.getDistanceFromWall(carPositionAtContact.space)
            val tMinus = secondsTillIntercept - wallDistanceAtIntercept / WALL_DEPART_SPEED
            val linedUp = Math.abs(correctionAngleRad) < Math.PI / 20

            return tMinus < 0.1 && tMinus > -.4 && linedUp
        }

        private fun isAcceptableZoneForWallTouch(bundle: TacticalBundle, ballPosition: Vector3): Boolean {
            val situation = bundle.tacticalSituation

            if (situation.gameMode == GameMode.SOCCER && ArenaModel.BACK_WALL - Math.abs(ballPosition.y) < 10) {
                // In soccer, don't go for touches on the back wall. High danger of accidentally driving
                // into the goal.
                if (Zone.isInDefensiveThird(bundle.zonePlan.ballZone, bundle.agentInput.team)) {
                    val car = bundle.agentInput.myCarData
                    if (Math.abs(car.position.x) > SoccerGoal.EXTENT && Math.abs(ballPosition.x) > SoccerGoal.EXTENT &&
                            car.position.x * ballPosition.x > 0) {
                        // Safe to do a wall touch because the car and the touch are both outside the goal frame.
                        return true
                    }
                }

                return false
            }

            val enemyGoal = GoalUtil.getEnemyGoal(bundle.agentInput.team)
            return (ballPosition.y - bundle.agentInput.myCarData.position.y) * enemyGoal.center.y > 0
        }

        fun hasWallTouchOpportunity(bundle: TacticalBundle): Boolean {

            val ballPath = bundle.tacticalSituation.ballPath
            val ballPredicate = { ballPosition: BallSlice ->
                isAcceptableZoneForWallTouch(bundle, ballPosition.space) && isBallOnWall(ballPosition) }

            val nearWallOption = ballPath.findSlice(ballPredicate) ?: return false

            val time = nearWallOption.time
            if (Duration.between(bundle.agentInput.time, time).seconds > 3) {
                return false // Not on wall soon enough
            }

            ballPath.getMotionAt(time.plusSeconds(1.0))?.let {
                return ballPredicate.invoke(it)
            }
            return false
        }
    }
}
