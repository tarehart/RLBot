package tarehart.rlbot.math.vector

import tarehart.rlbot.math.Plane

data class Vector3(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0) {

    val isZero: Boolean
        get() = x == 0.0 && y == 0.0 && z == 0.0

    operator fun plus(other: Vector3): Vector3 {
        return Vector3(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vector3): Vector3 {
        return Vector3(x - other.x, y - other.y, z - other.z)
    }

    fun scaled(scale: Double): Vector3 {
        return Vector3(x * scale, y * scale, z * scale)
    }

    /**
     * If magnitude is negative, we will return a vector facing the opposite direction.
     */
    fun scaledToMagnitude(magnitude: Double): Vector3 {
        if (isZero) {
            throw IllegalStateException("Cannot scale up a vector with length zero!")
        }
        val scaleRequired = magnitude / magnitude()
        return scaled(scaleRequired)
    }

    fun distance(other: Vector3): Double {
        val xDiff = x - other.x
        val yDiff = y - other.y
        val zDiff = z - other.z
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }

    fun magnitude(): Double {
        return Math.sqrt(magnitudeSquared())
    }

    fun magnitudeSquared(): Double {
        return x * x + y * y + z * z
    }

    fun normaliseCopy(): Vector3 {

        if (isZero) {
            throw IllegalStateException("Cannot normalize a vector with length zero!")
        }
        return this.scaled(1 / magnitude())
    }

    fun dotProduct(other: Vector3): Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun flatten(): Vector2 {
        return Vector2(x, y)
    }

    fun angle(v: Vector3): Double {
        val mag2 = magnitudeSquared()
        val vmag2 = v.magnitudeSquared()
        val dot = dotProduct(v)
        return Math.acos(dot / Math.sqrt(mag2 * vmag2))
    }

    fun crossProduct(v: Vector3): Vector3 {
        val tx = y * v.z - z * v.y
        val ty = z * v.x - x * v.z
        val tz = x * v.y - y * v.x
        return Vector3(tx, ty, tz)
    }

    fun projectToPlane(planeNormal: Vector3): Vector3 {
        val d = dotProduct(planeNormal)
        val antidote = planeNormal.scaled(-d)
        return plus(antidote)
    }

    fun shadowOntoPlane(plane: Plane): Vector3 {
        val distance = plane.distance(this)
        return this - plane.normal.scaledToMagnitude(distance)
    }

    override fun toString(): String {
        return ("(" + String.format("%.2f", x)
                + ", " + String.format("%.2f", y)
                + ", " + String.format("%.2f", z)
                + ")")
    }
}
