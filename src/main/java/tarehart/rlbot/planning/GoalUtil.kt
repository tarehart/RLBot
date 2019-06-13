package tarehart.rlbot.planning

import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath

object GoalUtil {

    private var BLUE_GOAL: Goal = SoccerGoal(true)
    private var ORANGE_GOAL: Goal = SoccerGoal(false)

    fun getOwnGoal(team: Team): Goal {
        return if (team == Team.BLUE) BLUE_GOAL else ORANGE_GOAL
    }

    fun getEnemyGoal(team: Team): Goal {
        return if (team == Team.BLUE) ORANGE_GOAL else BLUE_GOAL
    }

    fun transformNearPost(post: Vector2, ballPosition: Vector2): Vector2 {
        if (ballPosition.x * post.x < 0) {
            // It's actually the far post.
            return post
        }

        val ballCircle = Circle(ballPosition, ArenaModel.BALL_RADIUS.toDouble())

        ballCircle.calculateTangentPoints(post)?.let {
            val riskyTangent = it.toList().sortedBy { it.distance(post) }.first()
            return post + (ballPosition - riskyTangent)
        }

        return post
    }

    fun ballLingersInBox(goal: SoccerGoal, ballPath: BallPath): Boolean {
        val firstSlice = ballPath.findSlice({ slice -> goal.isInBox(slice.space) }, increment = 4)
        val secondSlice = firstSlice?.let { fs -> ballPath.getMotionAt(fs.time.plusSeconds(2.0)) }
        return secondSlice != null && goal.isInBox(secondSlice.space)
    }

    fun getNearestGoal(position: Vector3): Goal {
        return if (position.y > 0) ORANGE_GOAL else BLUE_GOAL
    }

    fun setHoopsGoals() {
        BLUE_GOAL = HoopsGoal(true)
        ORANGE_GOAL = HoopsGoal(false)
    }
}
