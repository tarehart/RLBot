package tarehart.rlbot.planning

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.Polygon
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath


class SoccerGoal(negativeSide: Boolean): Goal(negativeSide) {
    override val center: Vector3
    override val scorePlane: Plane

    private val box: Polygon

    init {

        center = Vector3(0.0, GOAL_DISTANCE * if (negativeSide) -1 else 1, 0.0)

        scorePlane = Plane(
                Vector3(0.0, (if (negativeSide) 1 else -1).toDouble(), 0.0),
                Vector3(0.0, (GOAL_DISTANCE + 2) * if (negativeSide) -1 else 1, 0.0))

        box = if (negativeSide) ZoneDefinitions.BLUEBOX else ZoneDefinitions.ORANGEBOX
    }


    override fun getNearestEntrance(ballPosition: Vector3, padding: Double): Vector3 {

        val adjustedExtent = EXTENT - ArenaModel.BALL_RADIUS.toDouble() - padding
        val adjustedHeight = GOAL_HEIGHT - ArenaModel.BALL_RADIUS.toDouble() - padding
        val x = Clamper.clamp(ballPosition.x, -adjustedExtent, adjustedExtent)
        val z = Clamper.clamp(ballPosition.z, ArenaModel.BALL_RADIUS.toDouble(), adjustedHeight)
        return Vector3(x, center.y, z)
    }

    /**
     * From shooter's perspective
     */
    override fun getLeftPost(padding: Double): Vector3 {
        return Vector3(center.x - (EXTENT - padding) * Math.signum(center.y), center.y, center.z)
    }

    /**
     * From shooter's perspective
     */
    override fun getRightPost(padding: Double): Vector3 {
        return Vector3(center.x + (EXTENT - padding) * Math.signum(center.y), center.y, center.z)
    }

    fun isInBox(position: Vector3): Boolean {
        return box.contains(position.flatten())
    }

    override fun predictGoalEvent(ballPath: BallPath): BallSlice? {
        return ballPath.getPlaneBreak(ballPath.startPoint.time, scorePlane, true)
    }

    companion object {

        private const val GOAL_DISTANCE = 102.0
        const val GOAL_HEIGHT = 12.0
        const val EXTENT = 17.8555
    }
}
