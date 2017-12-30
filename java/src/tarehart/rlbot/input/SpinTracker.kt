package tarehart.rlbot.input

import tarehart.rlbot.Bot
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3

class SpinTracker {

    private val previousOrientations = HashMap<Bot.Team, CarOrientation>()
    private val spins = HashMap<Bot.Team, CarSpin>()

    fun readInput(orientation: CarOrientation, team: Bot.Team, secondsElapsed: Double) {
        if (secondsElapsed > 0) {

            previousOrientations[team]?.let {
                spins.put(team, getCarSpin(it, orientation, secondsElapsed))
            }

            previousOrientations.put(team, orientation)
        }
    }

    private fun getCarSpin(prevData: CarOrientation, currData: CarOrientation, secondsElapsed: Double): CarSpin {

        val rateConversion = 1 / secondsElapsed

        val pitchAmount = getRotationAmount(currData.noseVector, prevData.roofVector)
        val yawAmount = getRotationAmount(currData.noseVector, prevData.rightVector)
        val rollAmount = getRotationAmount(currData.roofVector, prevData.rightVector)

        return CarSpin(pitchAmount * rateConversion, yawAmount * rateConversion, rollAmount * rateConversion)
    }

    private fun getRotationAmount(currentMoving: Vector3, previousOrthogonal: Vector3): Double {
        val projection = VectorUtil.project(currentMoving, previousOrthogonal)
        return Math.asin(projection.magnitude() * Math.signum(projection.dotProduct(previousOrthogonal)))
    }

    fun getSpin(team: Bot.Team): CarSpin {
        return spins[team] ?: CarSpin(0.0, 0.0, 0.0)
    }
}
