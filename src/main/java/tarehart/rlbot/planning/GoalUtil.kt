package tarehart.rlbot.planning

import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath

object GoalUtil {

    private val BLUE_GOAL = Goal(true)
    private val ORANGE_GOAL = Goal(false)

    fun getOwnGoal(team: Team): Goal {
        return if (team == Team.BLUE) BLUE_GOAL else ORANGE_GOAL
    }

    fun getEnemyGoal(team: Team): Goal {
        return if (team == Team.BLUE) ORANGE_GOAL else BLUE_GOAL
    }

    fun predictGoalEvent(goal: Goal, ballPath: BallPath): BallSlice? {
        return ballPath.getPlaneBreak(ballPath.startPoint.time, goal.scorePlane, true)
    }

    fun ballLingersInBox(goal: Goal, ballPath: BallPath): Boolean {
        val firstSlice = ballPath.findSlice({ slice -> goal.isInBox(slice.space) })
        val secondSlice = firstSlice?.let { fs -> ballPath.getMotionAt(fs.time.plusSeconds(2.0)) }
        return secondSlice != null && goal.isInBox(secondSlice.space)
    }

    fun getNearestGoal(position: Vector3): Goal {
        return if (position.y > 0) ORANGE_GOAL else BLUE_GOAL
    }
}
