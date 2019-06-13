package tarehart.rlbot.planning

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath


class HoopsGoal(negativeSide: Boolean): Goal(negativeSide) {
    override val center: Vector3
    override val scorePlane: Plane

    init {

        center = Vector3(0.0, GOAL_DISTANCE * if (negativeSide) -1 else 1, GOAL_HEIGHT)

        scorePlane = Plane(Vector3(0.0, 0.0, 1.0), center)
    }


    override fun getNearestEntrance(ballPosition: Vector3, padding: Double): Vector3 {

        val centerToBall = ballPosition - center
        val newRadius = RADIUS - padding
        val centerToEntrance = centerToBall.scaledToMagnitude(newRadius).flatten().toVector3()

        return center + centerToEntrance
    }

    /**
     * From shooter's perspective
     */
    override fun getLeftPost(padding: Double): Vector3 {
        return Vector3(center.x - (RADIUS - padding) * Math.signum(center.y), center.y, center.z)
    }

    /**
     * From shooter's perspective
     */
    override fun getRightPost(padding: Double): Vector3 {
        return Vector3(center.x + (RADIUS - padding) * Math.signum(center.y), center.y, center.z)
    }

    override fun predictGoalEvent(ballPath: BallPath): BallSlice? {
        return ballPath.getPlaneBreak(ballPath.startPoint.time, scorePlane, true,
                { v3 -> v3.flatten().distance(center.flatten()) < RADIUS }, increment = 4)
    }

    companion object {

        private const val GOAL_DISTANCE = 65.0
        const val GOAL_HEIGHT = 7.0
        const val RADIUS = 16.0
    }
}
