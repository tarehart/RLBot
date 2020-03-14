package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.ChipStrike
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import tarehart.rlbot.ui.DisplayFlags
import java.awt.Color
import java.util.*

object StandardSteering {


    fun steerTowardGroundPosition(car: CarData, position: Vector2,
                                  detourForBoost: Boolean = true, conserveBoost: Boolean = false): AgentOutput {

        if (ArenaModel.isCarOnWall(car)) {
            return SteerUtil.steerTowardPositionAcrossSeam(car, position.toVector3())
        }

        val myPositionFlat = car.position.flatten()
        val adjustedPosition: Vector2

        if (TacticsTelemetry[car.playerIndex]?.gameMode == GameMode.SOCCER &&
                crossesSoccerGoalLine(position, myPositionFlat)) {

            adjustedPosition = getAdjustedSoccerGoalCrossing(position, myPositionFlat)
            if(DisplayFlags[DisplayFlags.GOAL_CROSSING] == 1) {
                RenderUtil.drawSquare(car.renderer, Plane(Vector3.UP, adjustedPosition.toVector3()), 1, Color.RED)
            }

        } else {
            adjustedPosition =
                    if (!detourForBoost || car.boost > 99.0) position // This keeps us from swerving all around during unlimited boost games
                    else Optional.ofNullable(BoostAdvisor.getBoostWaypoint(car, position)).orElse(position)
        }

        val correctionAngle = SteerUtil.getCorrectionAngleRad(car, adjustedPosition)
        val distance = adjustedPosition.distance(myPositionFlat)
        val speed = ManeuverMath.forwardSpeed(car)
        return getSteeringOutput(correctionAngle, distance, speed, car.isSupersonic, conserveBoost, car.spin.yawRate)
    }

    fun steerTowardWallPosition(carData: CarData, position: Vector3): AgentOutput {
        val toPosition = position.minus(carData.position)
        val correctionAngle = VectorUtil.getCorrectionAngle(carData.orientation.noseVector, toPosition, carData.orientation.roofVector)
        val speed = carData.velocity.magnitude()
        val distance = position.distance(carData.position)
        return StandardSteering.getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic, false, carData.spin.yawRate)
    }

    private fun crossesSoccerGoalLine(p1: Vector2, p2: Vector2): Boolean {
        return Math.abs(p1.y) > ArenaModel.BACK_WALL || Math.abs(p2.y) > ArenaModel.BACK_WALL &&  // Something is past the goal line
                (Math.abs(p1.y) < ArenaModel.BACK_WALL || Math.abs(p2.y) < ArenaModel.BACK_WALL) // Something is not past the goal line
    }

    /**
     * Assumes that the two positions are on opposite sides of a goal line.
     */
    private fun getAdjustedSoccerGoalCrossing(p1: Vector2, p2: Vector2): Vector2 {
        val toTarget = p1 - p2
        val yLine = Math.signum(p1.y + p2.y) * ArenaModel.BACK_WALL
        val crossingFactor = (yLine - p2.y) / (p1.y - p2.y)
        val desiredExit = p2 + toTarget * crossingFactor
        val nearestGoal = GoalUtil.getNearestGoal(desiredExit.toVector3())
        return nearestGoal.getNearestEntrance(desiredExit.toVector3(), 2F).flatten()
    }

    private fun getSteeringOutput(correctionAngle: Float, distance: Float, speed: Float, isSupersonic: Boolean, conserveBoost: Boolean, yawRate: Float): AgentOutput {
        val difference = Math.abs(correctionAngle)
        val turnSharpness = difference * 6 / Math.PI + difference * speed * .1
        //turnSharpness = (1 - DEAD_ZONE) * turnSharpness + Math.signum(turnSharpness) * DEAD_ZONE;

        val yawRateVsIntent = yawRate * -Math.signum(correctionAngle)

        val shouldBrake = distance < 25 && difference > Math.PI / 4 && speed > 25 || speed > 20 && difference > Math.PI / 2
        val shouldSlide = yawRateVsIntent > 1 &&
                (speed < 30 && distance < 15 && difference > Math.PI / 2 || speed < 30 && difference > 3 * Math.PI / 4)
        val shouldBoost = !conserveBoost && !shouldBrake && difference < Math.PI / 6 && !isSupersonic

        return AgentOutput()
                .withThrottle(if (shouldBrake) -1.0 else 1.0)
                .withSteer(-Math.signum(correctionAngle) * turnSharpness)
                .withSlide(shouldSlide)
                .withBoost(shouldBoost)
    }

    fun steerTowardGroundPosition(carData: CarData, position: Vector3): AgentOutput {
        return steerTowardGroundPosition(carData, position.flatten(), detourForBoost = false)
    }

}
