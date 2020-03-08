package tarehart.rlbot.input

import tarehart.rlbot.math.vector.Vector3

class CarHitbox(val width: Float, val length: Float, val height: Float, val offset: Vector3) {
    val forwardExtent = length / 2 + offset.x
    val sidewaysExtent = width / 2
    val upwardExtent = height / 2 + offset.z

    companion object {
        // TODO: set accurate numbers.
        val OCTANE = CarHitbox(1F, 1F, 1F, Vector3())
    }
}
