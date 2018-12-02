package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.integration.metrics.DistanceMetric
import tarehart.rlbot.integration.metrics.VelocityMetric
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.time.Duration

class PlaneBreakAssert(val plane: Plane, val extent: Double, timeLimit: Duration, delayWhenBallFloating: Boolean = false): PacketAssert(timeLimit, delayWhenBallFloating) {
    override fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?) {
        val prevBallLoc = previousBundle?.agentInput?.ballPosition ?: return
        val ballLoc = bundle.agentInput.ballPosition

        val planeIntersection = VectorUtil.getPlaneIntersection(plane, prevBallLoc, ballLoc - prevBallLoc)

        planeIntersection?.let {
            val distance = it.distance(plane.position)
            if (distance <= extent) {
                if (negated) status = AssertStatus.FAILED
                else {
                    status = AssertStatus.SUCCEEDED
                    addMetric(DistanceMetric(distance))
                    val velocityMetric = VelocityMetric(bundle.agentInput.ballVelocity.magnitude(), "Ball Velocity")
                    addMetric(velocityMetric)
                }
            }
        }
    }

    companion object {
        val ENEMY_GOAL_PLANE = marchPlane(GoalUtil.getEnemyGoal(Team.BLUE).scorePlane, 1.0)
        val OWN_GOAL_PLANE = marchPlane(GoalUtil.getOwnGoal(Team.BLUE).scorePlane, 1.0)

        fun marchPlane(plane: Plane, distance: Double): Plane {
            return Plane(plane.normal,plane.position + plane.normal.scaled(distance))
        }
    }
}