package tarehart.rlbot.input

import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3

class SpinTracker {

    private val previousOrientations = HashMap<Int, CarOrientation>()
    private val spins = HashMap<Int, CarSpin>()

    fun readInput(orientation: CarOrientation, index: Int, secondsElapsed: Double) {
        if (secondsElapsed > 0) {

            previousOrientations[index]?.let {
                spins.put(index, getCarSpin(it, orientation, secondsElapsed))
            }

            previousOrientations.put(index, orientation)
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

    fun getSpin(index: Int): CarSpin {
        return spins[index] ?: CarSpin(0.0, 0.0, 0.0)
    }
}
