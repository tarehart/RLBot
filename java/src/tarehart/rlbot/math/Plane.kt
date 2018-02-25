package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector3

class Plane(normal: Vector3, val position: Vector3) {

    val normal = normal.normaliseCopy()

    fun distance(point: Vector3): Double {
        return (point - position).dotProduct(normal)
    }
}
