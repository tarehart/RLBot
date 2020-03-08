package tarehart.rlbot.planning

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import kotlin.math.min
import kotlin.math.sign


class HoopsGoal(negativeSide: Boolean): Goal(negativeSide) {
    override val center: Vector3
    override val scorePlane: Plane

    init {

        center = Vector3(0.0, GOAL_DISTANCE * if (negativeSide) -1 else 1, GOAL_HEIGHT)

        scorePlane = Plane(Vector3(0.0, 0.0, 1.0), center)
    }


    override fun getNearestEntrance(ballPosition: Vector3, padding: Number): Vector3 {

        val centerToBall = ballPosition - center
        val newRadius = min(RADIUS - padding.toFloat(), centerToBall.flatten().magnitude())
        val centerToEntrance = centerToBall.flatten().scaledToMagnitude(newRadius).toVector3()

        return center + centerToEntrance
    }

    /**
     * From shooter's perspective
     */
    override fun getLeftPost(padding: Number): Vector3 {
        return Vector3(center.x - (RADIUS - padding.toFloat()) * sign(center.y), center.y, center.z)
    }

    /**
     * From shooter's perspective
     */
    override fun getRightPost(padding: Number): Vector3 {
        return Vector3(center.x + (RADIUS - padding.toFloat()) * sign(center.y), center.y, center.z)
    }

    override fun predictGoalEvent(ballPath: BallPath): BallSlice? {
        return ballPath.getPlaneBreak(ballPath.startPoint.time, scorePlane, true,
                { v3 -> this.isGoalEvent(v3) }, increment = 4)
    }

    override fun isGoalEvent(planeBreakLocation: Vector3): Boolean {
        return planeBreakLocation.flatten().distance(center.flatten()) < RADIUS
    }

    companion object {

        private const val GOAL_DISTANCE = 65.0F
        const val GOAL_HEIGHT = 7.0F
        const val RADIUS = 16.0F
    }
}
