package tarehart.rlbot.planning

import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.Polygon
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel


class Goal(negativeSide: Boolean) {

    val center: Vector3
    val threatPlane: Plane
    val scorePlane: Plane
    val team: Team
    private val box: Polygon

    /**
     * From shooter's perspective
     */
    val leftPost: Vector3
        get() = getLeftPost(0.0)

    /**
     * From shooter's perspective
     */
    val rightPost: Vector3
        get() = getRightPost(0.0)

    init {

        center = Vector3(0.0, GOAL_DISTANCE * if (negativeSide) -1 else 1, 0.0)

        threatPlane = Plane(
                Vector3(0.0, (if (negativeSide) 1 else -1).toDouble(), 0.0),
                Vector3(0.0, (GOAL_DISTANCE - 1) * if (negativeSide) -1 else 1, 0.0))

        scorePlane = Plane(
                Vector3(0.0, (if (negativeSide) 1 else -1).toDouble(), 0.0),
                Vector3(0.0, (GOAL_DISTANCE + 2) * if (negativeSide) -1 else 1, 0.0))

        box = if (negativeSide) ZoneDefinitions.BLUEBOX else ZoneDefinitions.ORANGEBOX
        team = if (negativeSide) Team.BLUE else Team.ORANGE
    }


    fun getNearestEntrance(ballPosition: Vector3, padding: Double): Vector3 {

        val adjustedExtent = EXTENT - ArenaModel.BALL_RADIUS.toDouble() - padding
        val adjustedHeight = GOAL_HEIGHT - ArenaModel.BALL_RADIUS.toDouble() - padding
        val x = Math.min(adjustedExtent, Math.max(-adjustedExtent, ballPosition.x))
        val z = Math.min(adjustedHeight, Math.max(ArenaModel.BALL_RADIUS.toDouble(), ballPosition.z))
        return Vector3(x, center.y, z)
    }

    /**
     * From shooter's perspective
     */
    fun getLeftPost(padding: Double): Vector3 {
        return Vector3(center.x - (EXTENT - padding) * Math.signum(center.y), center.y, center.z)
    }

    /**
     * From shooter's perspective
     */
    fun getRightPost(padding: Double): Vector3 {
        return Vector3(center.x + (EXTENT - padding) * Math.signum(center.y), center.y, center.z)
    }

    fun isInBox(position: Vector3): Boolean {
        return box.contains(position.flatten())
    }

    companion object {

        private val GOAL_DISTANCE = 102.0
        val GOAL_HEIGHT = 12.0
        val EXTENT = 17.8555
    }
}
