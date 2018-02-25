package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil

class WallPass : KickStrategy {
    override fun getKickDirection(input: AgentInput): Vector3? {
        return getKickDirection(input, input.ballPosition)
    }

    override fun getKickDirection(input: AgentInput, ballPosition: Vector3): Vector3? {
        val car = input.myCarData
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall)
    }

    override fun getKickDirection(input: AgentInput, ballPosition: Vector3, easyKick: Vector3): Vector3? {
        return getDirection(input.myCarData, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall) != null
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3? {

        val bouncePlane = ArenaModel.getBouncePlane(ballPosition, Vector3(easyKick.x, easyKick.y, 0.0))
        val enemyGoalCenter = GoalUtil.getEnemyGoal(car.team).center
        val forward = easyKick.y * enemyGoalCenter.y > 0



        if (forward) {
            val sideWall = Math.abs(bouncePlane.normal.x) == 1.0
            val backWall = Math.abs(bouncePlane.normal.y) == 1.0
            val diagonalWall = !sideWall && !backWall

            if (sideWall) {
                return easyKick
            }

            if (diagonalWall && Vector2.angle(easyKick.flatten(), enemyGoalCenter.flatten()) < Math.PI / 8) {
                return easyKick
            }

            if (Math.abs(enemyGoalCenter.y - ballPosition.y) < 20) {
                return null
            }
        }

        if (Math.abs(enemyGoalCenter.y - ballPosition.y) < 20) {
            return null
        }

        // kick it straight to the side wall
        return Vector3(if (easyKick.x != 0.0) easyKick.x else 1.0, 0.0, 0.0).normaliseCopy()
    }
}
