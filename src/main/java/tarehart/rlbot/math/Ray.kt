package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector3

open class Ray(val position: Vector3, direction: Vector3) {
    val direction = direction.normaliseCopy()

    fun flatten(): Ray2 {
        return Ray2(position.flatten(), direction.flatten().normalized())
    }
}
