package tarehart.rlbot.steps.strikes

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.TacticsTelemetry

class WallPass : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3? {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall)
    }

    override fun getKickDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3? {
        return getDirection(car, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall) != null
    }

    override fun isShotOnGoal(): Boolean {
        return false
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3? {

        val bouncePlane = ArenaModel.getBouncePlane(ballPosition, Vector3(easyKick.x, easyKick.y, 0.0))

        if (Math.abs(bouncePlane.normal.y) > .99) {
            // No bouncing off the back wall for a wall pass!
            return null
        }

        val enemyGoal = GoalUtil.getEnemyGoal(car.team)
        val enemyGoalCenter = enemyGoal.center

        val goalDistanceFromWall = bouncePlane.distance(enemyGoalCenter)
        val strikeDistanceFromWall = bouncePlane.distance(ballPosition)
        val strikeClosenessRatio = strikeDistanceFromWall / (strikeDistanceFromWall + goalDistanceFromWall)

        val strikeShadowOnWall = bouncePlane.projectPoint(ballPosition)
        val goalShadowOnWall = bouncePlane.projectPoint(enemyGoalCenter)

        val strikeToGoalAlongWall = goalShadowOnWall - strikeShadowOnWall

        val target = strikeShadowOnWall + strikeToGoalAlongWall.scaled(strikeClosenessRatio)

        return target - ballPosition
    }
}
