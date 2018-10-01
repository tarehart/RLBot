package tarehart.rlbot.steps.state

import rlbot.gamestate.DesiredVector3
import tarehart.rlbot.math.vector.Vector3

class StateVector(x: Float?, y: Float?, z:Float?): DesiredVector3(
        x?.times(CONVERSION), y?.times(CONVERSION), z?.times(CONVERSION)) {

    companion object {
        val CONVERSION = Vector3.PACKET_DISTANCE_TO_CLASSIC.toFloat()
    }
}

