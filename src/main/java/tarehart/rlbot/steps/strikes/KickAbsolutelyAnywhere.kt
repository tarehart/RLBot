package tarehart.rlbot.steps.strikes

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil

class KickAbsolutelyAnywhere : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3 {
        return ballPosition.minus(car.position)
    }

    override fun getKickDirection(bundle: TacticalBundle, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        return easyKick
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        return true
    }

    override fun isShotOnGoal(): Boolean {
        return false
    }
}
