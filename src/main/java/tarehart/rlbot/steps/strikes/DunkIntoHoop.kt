package tarehart.rlbot.steps.strikes

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.HoopsGoal
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.TacticsTelemetry

class DunkIntoHoop : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3? {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall)
    }

    override fun getKickDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3? {
        return getDirection(car, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {

        if (ballPosition.z < HoopsGoal.GOAL_HEIGHT + 2 || car.boost < 30 ||
                ballPosition.flatten().distance(GoalUtil.getEnemyGoal(car.team).center.flatten()) > HoopsGoal.RADIUS * 3) {
            return false
        }

        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall) != null
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3? {

        return GoalUtil.getEnemyGoal(car.team).getNearestEntrance(ballPosition, 3.0) - car.position
    }
}
