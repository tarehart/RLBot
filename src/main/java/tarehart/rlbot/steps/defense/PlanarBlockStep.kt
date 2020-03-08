package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.intercept.strike.DoubleJumpPokeStrike
import tarehart.rlbot.intercept.strike.FlipHitStrike
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

class PlanarBlockStep: NestedPlanStep() {

    override fun getLocalSituation(): String {
        return "Planar block"
    }

    var launched = false

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        if (launched || Math.abs(car.position.y) > ArenaModel.BACK_WALL + 2 ||
                Math.abs(car.position.y) < ArenaModel.BACK_WALL - 10 || Math.abs(car.orientation.noseVector.x) < .8) {
            return null
        }

        val centerPlane = Plane(car.orientation.rightVector, car.position)
        val ballVelocityAlongNormal = VectorUtil.project(bundle.agentInput.ballVelocity, centerPlane.normal)

        val ballPosition = bundle.agentInput.ballPosition
        val isBallOnRight = centerPlane.distance(ballPosition) > 0
        val interceptPlane: Plane
        if (isBallOnRight) {
            interceptPlane = Plane(
                    car.orientation.rightVector,
                    car.position + car.orientation.rightVector.scaledToMagnitude(CONTACT_DISTANCE))
        } else {
            interceptPlane = Plane(
                    car.orientation.rightVector.scaled(-1F),
                    car.position - car.orientation.rightVector.scaledToMagnitude(CONTACT_DISTANCE))
        }

        val isBallApproachingPlane = ballVelocityAlongNormal.dotProduct(interceptPlane.normal) < 0
        if (!isBallApproachingPlane) {
            return null
        }

        val ballPath = bundle.tacticalSituation.ballPath
        val planeBreak = ballPath.getPlaneBreak(
                car.time, interceptPlane, directionSensitive = true) ?: GoalUtil.getOwnGoal(car.team).predictGoalEvent(ballPath) ?: return null

        val secondsTillIntercept = Duration.between(car.time, planeBreak.time).seconds
        val carAtIntercept = planeBreak.space - interceptPlane.normal.scaledToMagnitude(CONTACT_DISTANCE)
        val toIntercept = carAtIntercept - car.position
        val flatDistance = toIntercept.flatten().magnitude()
        val isForward = toIntercept.dotProduct(car.orientation.noseVector) > 0
        if (flatDistance > SoccerGoal.EXTENT * 2 ||
                isForward && secondsTillIntercept > 2) {
            return null
        }

        RenderUtil.drawSquare(car.renderer, interceptPlane, 3.0, Color.PINK)
        RenderUtil.drawSquare(car.renderer, interceptPlane, planeBreak.space.z, Color.PINK)
        RenderUtil.drawSphere(car.renderer, planeBreak.space, ArenaModel.BALL_RADIUS, Color.PINK)


        val forwardSpeed = ManeuverMath.forwardSpeed(car)
        val desiredSpeed = flatDistance * (if (isForward) 1.0 else -1.0) / secondsTillIntercept

        if (FlipHitStrike.isVerticallyAccessible(carAtIntercept.z) && secondsTillIntercept < .4) {
            val postDodgeSpeed = forwardSpeed + Math.signum(forwardSpeed) * 10
            if (flatDistance > postDodgeSpeed * secondsTillIntercept) {
                return startPlan(SetPieces.anyDirectionFlip(car, toIntercept.flatten()), bundle)
            }

            if (Math.abs(forwardSpeed) < 15) {
                return startPlan(SetPieces.anyDirectionFlip(car, (ballPosition - car.position).flatten()), bundle)
            }
        }

        val strike = DoubleJumpPokeStrike(carAtIntercept.z)

        strike.getPlan(car, SpaceTime(carAtIntercept, planeBreak.time))?.let {
            launched = true
            return startPlan(it, bundle)
        }



        if (secondsTillIntercept > 1 &&
                (isForward && desiredSpeed > AccelerationModel.SUPERSONIC_SPEED ||
                !isForward && Math.abs(desiredSpeed) > AccelerationModel.MEDIUM_SPEED)) {
            // Not reachable.
            return null
        }

        if (isForward) {
            return SteerUtil.steerTowardGroundPosition(car, car.position + Vector3(toIntercept.x))
                    .withThrottle(if (forwardSpeed > desiredSpeed) 0.0 else 1.0)
                    .withBoost(desiredSpeed - forwardSpeed > 5)
        } else {
            return SteerUtil.backUpTowardGroundPosition(car, (car.position + Vector3(toIntercept.x)).flatten())
                    .withThrottle(if (forwardSpeed < desiredSpeed) 0.0 else -1.0)
        }
    }

    companion object {
        private const val CONTACT_DISTANCE = ArenaModel.BALL_RADIUS + 1.5
    }
}
