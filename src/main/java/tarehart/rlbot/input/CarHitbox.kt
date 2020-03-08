package tarehart.rlbot.input

import tarehart.rlbot.math.vector.Vector3

class CarHitbox(val width: Float, val length: Float, val height: Float, val offset: Vector3) {
    val forwardExtent = length / 2 + offset.x
    val sidewaysExtent = width / 2
    val upwardExtent = height / 2 + offset.z

    companion object {
        val OCTANE = CarHitbox(1.6839882F, 2.3601475F, 0.7231815F, Vector3(0.2775132, 0, 0.41509974))
    }
}
