package tarehart.rlbot.steps.state

import rlbot.gamestate.DesiredVector3
import tarehart.rlbot.math.vector.Vector3

class StateVector(x: Number? = null, y: Number? = null, z:Number? = null): DesiredVector3(
        x?.toFloat()?.times(-CONVERSION), y?.toFloat()?.times(CONVERSION), z?.toFloat()?.times(CONVERSION)) {

    companion object {
        val CONVERSION = Vector3.PACKET_DISTANCE_TO_CLASSIC.toFloat()
        val ZERO = StateVector(0, 0, 0)
    }
}
