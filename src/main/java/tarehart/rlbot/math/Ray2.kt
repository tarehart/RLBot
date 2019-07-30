package tarehart.rlbot.math

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2

open class Ray2(val position: Vector2, direction: Vector2) {
    val direction = direction.normalized()

    companion object {

        /**
         * https://stackoverflow.com/a/2931703/280852
         */
        fun getIntersection(a: Ray2, b: Ray2): Vector2? {
            val dx = b.position.x - a.position.x
            val dy = b.position.y - a.position.y
            val det = b.direction.x * a.direction.y - b.direction.y * a.direction.x
            val u = (dy * b.direction.x - dx * b.direction.y) / det
            val v = (dy * a.direction.x - dx * a.direction.y) / det

            if (u < 0 || v < 0) {
                return null
            }

            return a.position + a.direction * u
        }

        fun fromCar(car: CarData): Ray2 {
            return Ray2(car.position.flatten(), car.orientation.noseVector.flatten())
        }
    }
}

