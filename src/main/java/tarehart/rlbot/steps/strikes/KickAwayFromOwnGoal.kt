package tarehart.rlbot.steps.strikes

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil

class KickAwayFromOwnGoal : KickStrategy {

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

    override fun isShotOnGoal(): Boolean {
        return false
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        val easyKickFlat = easyKick.flatten()
        val toLeftPost = GoalUtil.getOwnGoal(car.team).leftPost.minus(ballPosition).flatten()
        val toRightPost = GoalUtil.getOwnGoal(car.team).rightPost.minus(ballPosition).flatten()

        val safeDirectionRight = VectorUtil.rotateVector(toRightPost, -Math.PI / 8)
        val safeDirectionLeft = VectorUtil.rotateVector(toLeftPost, Math.PI / 8)

        val rightToEasyCorrection = safeDirectionRight.correctionAngle(easyKickFlat, false)
        val rightToLeftCorrection = safeDirectionRight.correctionAngle(safeDirectionLeft, false)
        val safeRightCorrection = easyKickFlat.correctionAngle(safeDirectionRight)
        val safeLeftCorrection = easyKickFlat.correctionAngle(safeDirectionLeft)
        val flatDirection = if (rightToLeftCorrection < rightToEasyCorrection) {
            // The easy kick is already wide. Go with the easy kick.
            Vector2(easyKickFlat.x, easyKickFlat.y)
        } else if (Math.abs(safeRightCorrection) < Math.abs(safeLeftCorrection)) {
            Vector2(safeDirectionRight.x, safeDirectionRight.y)
        } else {
            Vector2(safeDirectionLeft.x, safeDirectionLeft.y)
        }

        return flatDirection.normalized().withZ(.5)
    }
}
