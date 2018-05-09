package tarehart.rlbot.steps.strikes

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.TacticsAdvisor

class KickAtEnemyGoal : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3 {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall)
    }

    override fun getKickDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        return getDirection(car, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        return TacticsAdvisor.generousShotAngle(GoalUtil.getEnemyGoal(car.team), ballPosition.flatten(), car.playerIndex)
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        val easyKickFlat = easyKick.flatten()
        val toLeftCorner = GoalUtil.getEnemyGoal(car.team).getLeftPost(6.0).minus(ballPosition).flatten()
        val toRightCorner = GoalUtil.getEnemyGoal(car.team).getRightPost(6.0).minus(ballPosition).flatten()

        val rightCornerCorrection = easyKickFlat.correctionAngle(toRightCorner)
        val leftCornerCorrection = easyKickFlat.correctionAngle(toLeftCorner)
        return if (rightCornerCorrection < 0 && leftCornerCorrection > 0) {
            // The easy kick is already on target. Go with the easy kick.
            Vector3(easyKickFlat.x, easyKickFlat.y, 0.0)
        } else if (Math.abs(rightCornerCorrection) < Math.abs(leftCornerCorrection)) {
            Vector3(toRightCorner.x, toRightCorner.y, 0.0)
        } else {
            Vector3(toLeftCorner.x, toLeftCorner.y, 0.0)
        }
    }
}
