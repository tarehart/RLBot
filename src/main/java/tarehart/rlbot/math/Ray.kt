package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector3

open class Ray(val position: Vector3, direction: Vector3) {
    val direction = direction.normaliseCopy()
}
