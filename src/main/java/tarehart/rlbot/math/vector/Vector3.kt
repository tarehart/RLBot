package tarehart.rlbot.math.vector

import com.google.flatbuffers.FlatBufferBuilder
import tarehart.rlbot.math.Plane
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class Vector3(x: Number = 0, y: Number = 0, z: Number = 0): rlbot.vector.Vector3(x.toFloat(), y.toFloat(), z.toFloat()) {
    val isZero = (x == 0 && y == 0 && z == 0)

    constructor(flatVec: rlbot.flat.Vector3): this(
            -flatVec.x() / PACKET_DISTANCE_TO_CLASSIC,
            flatVec.y() / PACKET_DISTANCE_TO_CLASSIC,
            flatVec.z() / PACKET_DISTANCE_TO_CLASSIC)

    operator fun plus(other: Vector3): Vector3 {
        return Vector3(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vector3): Vector3 {
        return Vector3(x - other.x, y - other.y, z - other.z)
    }

    operator fun div(value: Number): Vector3 {
        return Vector3(x / value.toFloat(), y / value.toFloat(), z / value.toFloat())
    }

    operator fun div(value: Vector3): Vector3 {
        return Vector3(x / value.x, y / value.y, z / value.z)
    }

    operator fun times(value: Number): Vector3 {
        return Vector3(x * value.toFloat(), y * value.toFloat(), z * value.toFloat())
    }

    operator fun times(value: Vector3): Vector3 {
        return Vector3(x * value.x, y * value.y, z * value.z)
    }

    operator fun get(index: Int): Float {
        if (index == 0)
            return x
        if (index == 1)
            return y
        if (index == 2)
            return z
        return 0F
    }

    fun scaled(scale: Number): Vector3 {
        val s = scale.toFloat()
        return Vector3(x * s, y * s, z * s)
    }

    fun withX(x: Number): Vector3 {
        return Vector3(x, y, z)
    }

    fun withY(y: Number): Vector3 {
        return Vector3(x, y, z)
    }

    fun withZ(z: Number): Vector3 {
        return Vector3(x, y, z)
    }

    /**
     * If magnitude is negative, we will return a vector facing the opposite direction.
     */
    fun scaledToMagnitude(magnitude: Number): Vector3 {
        if (isZero) {
            throw IllegalStateException("Cannot scale up a vector with length zero!")
        }
        val scaleRequired = magnitude.toFloat() / magnitude()
        return scaled(scaleRequired)
    }

    fun distance(other: Vector3): Float {
        return sqrt(distanceSquared(other))
    }

    fun distanceSquared(other: Vector3): Float {
        val xDiff = x - other.x
        val yDiff = y - other.y
        val zDiff = z - other.z
        return (xDiff * xDiff + yDiff * yDiff + zDiff * zDiff)
    }

    fun magnitude(): Float {
        return sqrt(magnitudeSquared())
    }

    fun magnitudeSquared(): Float {
        return (x * x + y * y + z * z)
    }

    fun normaliseCopy(): Vector3 {

        if (isZero) {
            throw IllegalStateException("Cannot normalize a vector with length zero!")
        }
        return this.scaled(1 / magnitude())
    }

    fun dotProduct(other: Vector3): Float {
        return (x * other.x + y * other.y + z * other.z)
    }

    fun flatten(): Vector2 {
        return Vector2(x, y)
    }

    fun angle(v: Vector3): Float {
        val mag2 = magnitudeSquared()
        val vmag2 = v.magnitudeSquared()
        val dot = dotProduct(v)
        return acos(dot / sqrt(mag2 * vmag2))
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

    override fun toFlatbuffer(builder: FlatBufferBuilder?): Int {
        return rlbot.flat.Vector3.createVector3(builder,
                -x * PACKET_DISTANCE_TO_CLASSIC,
                y * PACKET_DISTANCE_TO_CLASSIC,
                z * PACKET_DISTANCE_TO_CLASSIC)
    }

    override fun toString(): String {
        return ("(" + String.format("%.2f", x)
                + ", " + String.format("%.2f", y)
                + ", " + String.format("%.2f", z)
                + ")")
    }

    override fun equals(other: Any?): Boolean {
        val o = other as? Vector3 ?: return false
        return o.x == x && o.y == y && o.z == z
    }

    companion object {

        const val PACKET_DISTANCE_TO_CLASSIC = 50
        val UP = Vector3(0.0, 0.0, 1.0)
        val DOWN = Vector3(0.0, 0.0, -1.0)
        val ZERO = Vector3()

        fun fromRlbot(v: rlbot.flat.Vector3): Vector3 {
            // Invert the X value so that the axes make more sense.
            return Vector3(-v.x() / PACKET_DISTANCE_TO_CLASSIC, v.y() / PACKET_DISTANCE_TO_CLASSIC, v.z() / PACKET_DISTANCE_TO_CLASSIC)
        }
    }
}
