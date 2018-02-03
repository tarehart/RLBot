package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.Step
import tarehart.rlbot.time.Duration

import java.awt.*

import tarehart.rlbot.tuning.BotLog.println

class WallTouchStep : Step {
    private var originalIntercept: Vector3? = null

    override val situation: String
        get() = "Making a wall touch."

    override fun getOutput(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        if (!car.hasWheelContact) {
            println("Failed to make the wall touch because the car has no wheel contact", input.playerIndex)
            return null
        }


        val ballPath = ArenaModel.predictBallPath(input)
        val fullAcceleration = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost, 0.0)

        val interceptOpportunity = InterceptCalculator.getFilteredInterceptOpportunity(
                car, ballPath, fullAcceleration, Vector3(), { c: CarData, ballPosition: SpaceTime -> isBallOnWall(c, ballPosition) })
        val motion = interceptOpportunity?.let{ ballPath.getMotionAt(it.time) }


        if (motion == null) {
            println("Failed to make the wall touch because we see no intercepts on the wall", input.playerIndex)
            return null
        }


        originalIntercept?.let {
            if (it.distance(motion.space) > 20) {
                println("Failed to make the wall touch because the intercept changed", input.playerIndex)
                return null // Failed to kick it soon enough, new stuff has happened.
            }
        }

        val plane = ArenaModel.getNearestPlane(motion.space)
        if (plane.normal.z == 1.0) {
            println("Failed to make the wall touch because the ball is now close to the ground", input.playerIndex)
            return null
        }

        if (originalIntercept == null) {
            originalIntercept = motion.space
        }

        if (readyToJump(input, motion.toSpaceTime())) {
            println("Jumping for wall touch.", input.playerIndex)
            return AgentOutput().withThrottle(1.0).withJump()
        }

        return SteerUtil.steerTowardPositionAcrossSeam(car, motion.space)
    }

    override fun canInterrupt(): Boolean {
        return true
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {
        val ACCEPTABLE_WALL_DISTANCE = (ArenaModel.BALL_RADIUS + 5).toDouble()
        val WALL_DEPART_SPEED = 10.0
        private val MIN_HEIGHT = 6.0

        private fun isBallOnWall(car: CarData, ballPosition: SpaceTime): Boolean {
            return ballPosition.space.z > MIN_HEIGHT && ArenaModel.getDistanceFromWall(ballPosition.space) <= ACCEPTABLE_WALL_DISTANCE
        }

        private fun isBallOnWall(ballPosition: BallSlice): Boolean {
            return ballPosition.space.z > MIN_HEIGHT && ArenaModel.getDistanceFromWall(ballPosition.space) <= ACCEPTABLE_WALL_DISTANCE
        }

        private fun readyToJump(input: AgentInput, carPositionAtContact: SpaceTime): Boolean {

            if (ArenaModel.getDistanceFromWall(carPositionAtContact.space) > ArenaModel.BALL_RADIUS + .7) {
                return false // Really close to wall, no need to jump. Just chip it.
            }
            val car = input.myCarData
            val toPosition = carPositionAtContact.space.minus(car.position)
            val correctionAngleRad = VectorUtil.getCorrectionAngle(car.orientation.noseVector, toPosition, car.orientation.roofVector)
            val secondsTillIntercept = Duration.between(input.time, carPositionAtContact.time).seconds
            val wallDistanceAtIntercept = ArenaModel.getDistanceFromWall(carPositionAtContact.space)
            val tMinus = secondsTillIntercept - wallDistanceAtIntercept / WALL_DEPART_SPEED
            val linedUp = Math.abs(correctionAngleRad) < Math.PI / 8
            if (tMinus < 3) {
                println("Correction angle: " + correctionAngleRad, input.playerIndex)
            }

            return tMinus < 0.1 && tMinus > -.4 && linedUp
        }

        fun hasWallTouchOpportunity(input: AgentInput, ballPath: BallPath): Boolean {

            val nearWallOption = ballPath.findSlice { ballPosition: BallSlice -> isBallOnWall(ballPosition) }
            if (nearWallOption != null) {
                val time = nearWallOption.time
                if (Duration.between(input.time, time).seconds > 3) {
                    return false // Not on wall soon enough
                }

                val ballLater = ballPath.getMotionAt(time.plusSeconds(1.0))
                if (ballLater != null) {
                    val (space) = ballLater
                    if (ArenaModel.getDistanceFromWall(space) > ACCEPTABLE_WALL_DISTANCE) {
                        return false
                    }
                    val ownGoalCenter = GoalUtil.getOwnGoal(input.team).center
                    return space.distance(ownGoalCenter) > input.myCarData.position.distance(ownGoalCenter)
                }

            }

            return false
        }
    }
}
