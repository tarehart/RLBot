package tarehart.rlbot.math.vector

import tarehart.rlbot.math.Atan
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.math.VectorUtil

data class Vector2(val x: Double, val y: Double) {

    constructor(x: Number, y: Number): this(x.toDouble(), y.toDouble())

    val isZero: Boolean
        get() = x == 0.0 && y == 0.0

    operator fun plus(other: Vector2): Vector2 {
        return Vector2(x + other.x, y + other.y)
    }

    operator fun minus(other: Vector2): Vector2 {
        return Vector2(x - other.x, y - other.y)
    }

    operator fun times(value: Double): Vector2 {
        return Vector2(x * value, y * value)
    }

    fun scaled(scale: Double): Vector2 {
        return Vector2(x * scale, y * scale)
    }

    /**
     * If magnitude is negative, we will return a vector facing the opposite direction.
     */
    fun scaledToMagnitude(magnitude: Double): Vector2 {
        if (isZero) {
            throw IllegalStateException("Cannot scale up a vector with length zero!")
        }
        val scaleRequired = magnitude / magnitude()
        return scaled(scaleRequired)
    }

    fun distance(other: Vector2): Double {
        val xDiff = x - other.x
        val yDiff = y - other.y
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff)
    }

    fun magnitude(): Double {
        return Math.sqrt(magnitudeSquared())
    }

    fun magnitudeSquared(): Double {
        return x * x + y * y
    }

    fun normalized(): Vector2 {

        if (isZero) {
            throw IllegalStateException("Cannot normalize a vector with length zero!")
        }
        return this.scaled(1 / magnitude())
    }

    fun dotProduct(other: Vector2): Double {
        return x * other.x + y * other.y
    }

    fun correctionAngle(ideal: Vector2): Double {

        if (isZero || ideal.isZero) {
            return 0.0
        }

        // https://stackoverflow.com/questions/2150050/finding-signed-angle-between-vectors
        return Atan.atan2( this.x * ideal.y - this.y * ideal.x, this.x * ideal.x + this.y * ideal.y )
    }

    fun correctionAngle(ideal: Vector2, clockwise: Boolean): Double {

        if (isZero || ideal.isZero) {
            return 0.0
        }

        var currentRad = Atan.atan2(y, x)
        var idealRad = Atan.atan2(ideal.y, ideal.x)

        if ((idealRad - currentRad) > 0 && clockwise) {
            currentRad += Math.PI * 2
        }
        if ((idealRad - currentRad) < 0 && !clockwise) {
            idealRad += Math.PI * 2
        }

        return idealRad - currentRad
    }

    /**
     * This vector will be rotated towards the ideal vector, but will move no further than
     * maxRotation.
     */
    fun rotateTowards(ideal: Vector2, maxRotation: Double): Vector2 {
        val correctionAngle = correctionAngle(ideal)
        if (Math.abs(correctionAngle) < maxRotation) {
            return ideal.scaledToMagnitude(this.magnitude())
        }
        val tolerantCorrection = Clamper.clamp(correctionAngle, -maxRotation, maxRotation)
        return VectorUtil.rotateVector(this, tolerantCorrection)
    }

    override fun toString(): String {
        return ("(" + String.format("%.2f", x)
                + ", " + String.format("%.2f", y)
                + ")")
    }

    fun toVector3(): Vector3 {
        return withZ(0.0)
    }

    fun withZ(z: Double): Vector3 {
        return Vector3(x, y, z)
    }

    companion object {

        /**
         * Will always return a positive value <= Math.PI
         */
        fun angle(a: Vector2, b: Vector2): Double {
            return Math.abs(a.correctionAngle(b))
        }

        fun alignment(start: Vector2, middle: Vector2, end: Vector2): Double {
            val startToMiddle = middle - start
            val middleToEnd = end - middle
            return startToMiddle.normalized().dotProduct(middleToEnd.normalized())
        }

        val ZERO = Vector2(0.0, 0.0)
    }
}
