package tarehart.rlbot.steps.strikes

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Goal
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.tactics.SoccerTacticsAdvisor
import tarehart.rlbot.tactics.TacticsAdvisor

class KickAtEnemyGoal : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3 {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall)
    }

    override fun getKickDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        return getDirection(car, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {

        if (ballPosition.z > SoccerGoal.GOAL_HEIGHT) {
            if (Math.abs(ballPosition.y - GoalUtil.getEnemyGoal(car.team).center.y) < ballPosition.z - SoccerGoal.GOAL_HEIGHT) {
                // Ball is too tight above the crossbar, can't angle it down.
                return false
            }
        }

        return SoccerTacticsAdvisor.generousShotAngle(GoalUtil.getEnemyGoal(car.team), ballPosition.flatten())
    }

    override fun isShotOnGoal(): Boolean {
        return true
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        val easyKickFlat = easyKick.flatten()

        val ballPositionFlat = ballPosition.flatten()

        val leftPost = GoalUtil.transformNearPost(
                GoalUtil.getEnemyGoal(car.team).getLeftPost(6.0).flatten(),
                ballPositionFlat)

        val rightPost = GoalUtil.transformNearPost(
                GoalUtil.getEnemyGoal(car.team).getRightPost(6.0).flatten(),
                ballPositionFlat)

        val toLeftCorner = leftPost - ballPositionFlat
        val toRightCorner = rightPost - ballPositionFlat

        val rightCornerCorrection = easyKickFlat.correctionAngle(toRightCorner)
        val leftCornerCorrection = easyKickFlat.correctionAngle(toLeftCorner)
        val flatDirection = if (rightCornerCorrection < 0 && leftCornerCorrection > 0) {
            // The easy kick is already on target. Go with the easy kick.
            Vector2(easyKickFlat.x, easyKickFlat.y)
        } else if (Math.abs(rightCornerCorrection) < Math.abs(leftCornerCorrection)) {
            Vector2(toRightCorner.x, toRightCorner.y)
        } else {
            Vector2(toLeftCorner.x, toLeftCorner.y)
        }

        // Kick it high if we're far away from the goal, maybe we can lob the opponent.
        val loft = if (toLeftCorner.magnitude() > 50) .4 else 0.0

        return flatDirection.normalized().withZ(loft)
    }
}
