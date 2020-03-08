package tarehart.rlbot.math.vector

import tarehart.rlbot.math.Atan
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.math.VectorUtil
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

data class Vector2(val x: Float, val y: Float) {

    constructor(x: Number, y: Number): this(x.toFloat(), y.toFloat())

    val isZero: Boolean
        get() = x == 0F && y == 0F

    operator fun plus(other: Vector2): Vector2 {
        return Vector2(x + other.x, y + other.y)
    }

    operator fun minus(other: Vector2): Vector2 {
        return Vector2(x - other.x, y - other.y)
    }

    operator fun times(value: Float): Vector2 {
        return Vector2(x * value, y * value)
    }

    fun scaled(scale: Float): Vector2 {
        return Vector2(x * scale, y * scale)
    }

    /**
     * If magnitude is negative, we will return a vector facing the opposite direction.
     */
    fun scaledToMagnitude(magnitude: Number): Vector2 {
        if (isZero) {
            throw IllegalStateException("Cannot scale up a vector with length zero!")
        }
        val scaleRequired = magnitude.toFloat() / magnitude()
        return scaled(scaleRequired)
    }

    fun distance(other: Vector2): Float {
        val xDiff = x - other.x
        val yDiff = y - other.y
        return sqrt(xDiff * xDiff + yDiff * yDiff)
    }

    fun magnitude(): Float {
        return sqrt(magnitudeSquared())
    }

    fun magnitudeSquared(): Float {
        return x * x + y * y
    }

    fun normalized(): Vector2 {

        if (isZero) {
            throw IllegalStateException("Cannot normalize a vector with length zero!")
        }
        return this.scaled(1 / magnitude())
    }

    fun dotProduct(other: Vector2): Float {
        return x * other.x + y * other.y
    }

    fun correctionAngle(ideal: Vector2): Float {

        if (isZero || ideal.isZero) {
            return 0F
        }

        // https://stackoverflow.com/questions/2150050/finding-signed-angle-between-vectors
        return Atan.atan2( this.x * ideal.y - this.y * ideal.x, this.x * ideal.x + this.y * ideal.y )
    }

    fun correctionAngle(ideal: Vector2, clockwise: Boolean): Float {

        if (isZero || ideal.isZero) {
            return 0F
        }

        var currentRad = Atan.atan2(y, x)
        var idealRad = Atan.atan2(ideal.y, ideal.x)

        if ((idealRad - currentRad) > 0 && clockwise) {
            currentRad += PI.toFloat() * 2
        }
        if ((idealRad - currentRad) < 0 && !clockwise) {
            idealRad += PI.toFloat() * 2
        }

        return idealRad - currentRad
    }

    /**
     * This vector will be rotated towards the ideal vector, but will move no further than
     * maxRotation.
     */
    fun rotateTowards(ideal: Vector2, maxRotation: Number): Vector2 {
        val maxRot = maxRotation.toFloat()
        val correctionAngle = correctionAngle(ideal)
        if (Math.abs(correctionAngle) < maxRot) {
            return ideal.scaledToMagnitude(this.magnitude())
        }
        val tolerantCorrection = Clamper.clamp(correctionAngle, -maxRot, maxRot)
        return VectorUtil.rotateVector(this, tolerantCorrection)
    }

    override fun toString(): String {
        return ("(" + String.format("%.2f", x)
                + ", " + String.format("%.2f", y)
                + ")")
    }

    fun toVector3(): Vector3 {
        return withZ(0)
    }

    fun withZ(z: Number): Vector3 {
        return Vector3(x, y, z)
    }

    companion object {

        /**
         * Will always return a positive value <= Math.PI
         */
        fun angle(a: Vector2, b: Vector2): Float {
            return abs(a.correctionAngle(b))
        }

        /**
         * Returns 1.0 for a completely aligned series of points.
         * Returns -1.0 if it doubles back completely.
         * 0.0 for a right angle.
         */
        fun alignment(start: Vector2, middle: Vector2, end: Vector2): Float {
            val startToMiddle = middle - start
            val middleToEnd = end - middle
            if (startToMiddle.isZero || middleToEnd.isZero) {
                return 1F
            }
            return startToMiddle.normalized().dotProduct(middleToEnd.normalized())
        }

        val ZERO = Vector2(0.0, 0.0)
    }
}
