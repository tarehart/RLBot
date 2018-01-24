package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil

class KickAwayFromOwnGoal : KickStrategy {


    override fun getKickDirection(input: AgentInput): Vector3 {
        return getKickDirection(input, input.ballPosition)
    }

    override fun getKickDirection(input: AgentInput, ballPosition: Vector3): Vector3 {
        val car = input.myCarData
        val toBall = ballPosition.minus(car.position)
        return getDirection(input.myCarData, ballPosition, toBall)
    }

    override fun getKickDirection(input: AgentInput, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        return getDirection(input.myCarData, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        return true
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        val easyKickFlat = easyKick.flatten()
        val toLeftPost = GoalUtil.getOwnGoal(car.team).leftPost.minus(ballPosition).flatten()
        val toRightPost = GoalUtil.getOwnGoal(car.team).rightPost.minus(ballPosition).flatten()

        val safeDirectionRight = VectorUtil.rotateVector(toRightPost, -Math.PI / 4)
        val safeDirectionLeft = VectorUtil.rotateVector(toLeftPost, Math.PI / 4)

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
