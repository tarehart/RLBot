package tarehart.rlbot.math.vector

import tarehart.rlbot.math.Plane
import java.util.*
import kotlin.math.abs

data class Vector3(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0): Iterable<Double> {
    val isZero = (x == 0.0 && y == 0.0 && z == 0.0)
    private val list =  listOf(x, y, z)



    operator fun plus(other: Vector3): Vector3 {
        return Vector3(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vector3): Vector3 {
        return Vector3(x - other.x, y - other.y, z - other.z)
    }

    operator fun div(value: Double): Vector3 {
        return Vector3(x / value, y / value, z / value)
    }

    operator fun div(value: Vector3): Vector3 {
        return Vector3(x / value.x, y / value.y, z / value.z)
    }

    operator fun times(value: Double): Vector3 {
        return Vector3(x * value, y * value, z * value)
    }

    operator fun times(value: Vector3): Vector3 {
        return Vector3(x * value.x, y * value.y, z * value.z)
    }

    operator fun get(index: Int): Double {
        return list[index]
    }

    override fun iterator(): Iterator<Double> {
        return list.iterator()
    }

    fun scaled(scale: Double): Vector3 {
        return Vector3(x * scale, y * scale, z * scale)
    }

    fun withX(x: Double): Vector3 {
        return Vector3(x, y, z)
    }

    fun withY(y: Double): Vector3 {
        return Vector3(x, y, z)
    }

    fun withZ(z: Double): Vector3 {
        return Vector3(x, y, z)
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
        return Math.sqrt(distanceSquared(other))
    }

    fun distanceSquared(other: Vector3): Double {
        val xDiff = x - other.x
        val yDiff = y - other.y
        val zDiff = z - other.z
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff
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

    fun abs(): Vector3 {
        return Vector3(abs(x), abs(y), abs(z))
    }

    // TODO: I'm not sure I like this, I'm using it in HoopsZones and its an unclear and specific function.
    fun withinTolerance(tolerance: Vector3): Boolean {
        val absolute = abs()
        return absolute.x <= tolerance.x && absolute.y <= tolerance.y && absolute.z <= tolerance.z
    }

    fun toRlbot(): rlbot.vector.Vector3 {
        // Invert x because rlbot uses left-handed coordinates
        return rlbot.vector.Vector3(
                (-x * PACKET_DISTANCE_TO_CLASSIC).toFloat(),
                (y * PACKET_DISTANCE_TO_CLASSIC).toFloat(),
                (z * PACKET_DISTANCE_TO_CLASSIC).toFloat())
    }

    override fun toString(): String {
        return ("(" + String.format("%.2f", x)
                + ", " + String.format("%.2f", y)
                + ", " + String.format("%.2f", z)
                + ")")
    }

    companion object {

        const val PACKET_DISTANCE_TO_CLASSIC = 50.0
        val UP = Vector3(0.0, 0.0, 1.0)
        val ZERO = Vector3()

        fun fromRlbot(v: rlbot.flat.Vector3): Vector3 {
            // Invert the X value so that the axes make more sense.
            return Vector3(-v.x() / PACKET_DISTANCE_TO_CLASSIC, v.y() / PACKET_DISTANCE_TO_CLASSIC, v.z() / PACKET_DISTANCE_TO_CLASSIC)
        }
    }
}
