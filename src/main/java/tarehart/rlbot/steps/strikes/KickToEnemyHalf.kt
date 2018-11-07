package tarehart.rlbot.steps.strikes

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil

class KickToEnemyHalf : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3 {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall)
    }

    override fun getKickDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        return getDirection(car, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        return true
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        val easyKickFlat = easyKick.flatten()
        val leftSign = Math.signum(GoalUtil.getOwnGoal(car.team).leftPost.x)

        val toLeftPost = Vector3(leftSign * ArenaModel.SIDE_WALL, 0.0, 0.0).minus(ballPosition).flatten()
        val toRightPost = Vector3(-leftSign * ArenaModel.SIDE_WALL, 0.0, 0.0).minus(ballPosition).flatten()

        val safeDirectionRight = VectorUtil.rotateVector(toRightPost, -Math.PI / 8)
        val safeDirectionLeft = VectorUtil.rotateVector(toLeftPost, Math.PI / 8)

        val rightToEasyCorrection = safeDirectionRight.correctionAngle(easyKickFlat, false)
        val rightToLeftCorrection = safeDirectionRight.correctionAngle(safeDirectionLeft, false)
        val safeRightCorrection = easyKickFlat.correctionAngle(safeDirectionRight)
        val safeLeftCorrection = easyKickFlat.correctionAngle(safeDirectionLeft)
        return if (rightToLeftCorrection < rightToEasyCorrection) {
            // The easy kick is already wide. Go with the easy kick.
            Vector3(easyKickFlat.x, easyKickFlat.y, 0.0)
        } else if (Math.abs(safeRightCorrection) < Math.abs(safeLeftCorrection)) {
            Vector3(safeDirectionRight.x, safeDirectionRight.y, 0.0)
        } else {
            Vector3(safeDirectionLeft.x, safeDirectionLeft.y, 0.0)
        }
    }

}
