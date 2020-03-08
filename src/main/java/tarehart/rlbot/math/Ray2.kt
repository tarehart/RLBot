package tarehart.rlbot.math

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import kotlin.math.pow
import kotlin.math.sqrt

open class Ray2(val position: Vector2, direction: Vector2) {
    val direction = direction.normalized()

    /**
     * Taken from https://math.stackexchange.com/a/311956/550722
     */
    fun firstCircleIntersection(circle: Circle): Vector2? {
        val a = direction.magnitudeSquared()
        val b = 2 * direction.x * (position.x - circle.center.x) + 2 * direction.y * (position.y - circle.center.y)
        val c = square(position.x - circle.center.x) + square(position.y - circle.center.y) - square(circle.radius)
        val discrim = b * b - 4 * a * c
        if (discrim < 0) {
            return null
        }
        val t = 2 * c / (-b + sqrt(discrim))
        if (t < 0) {
            return null
        }
        return position + direction * t
    }

    private fun square(n: Float): Float {
        return n * n
    }

    companion object {

        /**
         * https://stackoverflow.com/a/2931703/280852
         *
         * Returns the intersection point if it exists, followed by the distance of that intersection
         * along ray A.
         */
        fun getIntersection(a: Ray2, b: Ray2): Pair<Vector2?, Float> {
            val dx = b.position.x - a.position.x
            val dy = b.position.y - a.position.y
            val det = b.direction.x * a.direction.y - b.direction.y * a.direction.x
            val u = (dy * b.direction.x - dx * b.direction.y) / det
            val v = (dy * a.direction.x - dx * a.direction.y) / det

            if (u < 0 || v < 0) {
                return Pair(null, u)
            }

            return Pair(a.position + a.direction * u, u)
        }

        fun fromCar(car: CarData): Ray2 {
            return Ray2(car.position.flatten(), car.orientation.noseVector.flatten())
        }
    }
}

