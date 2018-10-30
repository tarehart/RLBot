package tarehart.rlbot.math

import org.ejml.simple.SimpleMatrix
import tarehart.rlbot.math.vector.Vector3
import kotlin.math.abs


class Mat3(private val matrix: SimpleMatrix) {

    constructor(values: Array<DoubleArray>): this(SimpleMatrix(values))

    fun transpose(): Mat3 {
        return Mat3(matrix.transpose())
    }

    fun dot(vec: Vector3): Vector3 {
        return toVec(matrix.mult(toMatrix(vec)))
    }

    fun dot(mat: Mat3): Mat3 {
        return Mat3(matrix.mult(mat.matrix))
    }

    fun trace(): Double {
        return matrix.trace()
    }

    fun get(row: Int, col: Int): Double {
        return matrix.get(row, col)
    }

    operator fun plus(mat: Mat3): Mat3 {
        return Mat3(matrix.plus(mat.matrix))
    }

    operator fun times(value: Double): Mat3 {
        return Mat3(matrix.scale(value))
    }

    companion object {
        val IDENTITY = Mat3(SimpleMatrix.identity(3))

        private fun toMatrix(vec: Vector3): SimpleMatrix {
            return SimpleMatrix(arrayOf(doubleArrayOf(vec.x), doubleArrayOf(vec.y), doubleArrayOf(vec.z)))
        }

        private fun toVec(matrix: SimpleMatrix): Vector3 {
            return Vector3(matrix.get(0), matrix.get(1), matrix.get(2))
        }

        fun lookingTo(direction: Vector3, up: Vector3 = Vector3.UP): Mat3 {
            val forward = direction.normaliseCopy()
            val safeUp = if (abs(direction.z) == 1.0) Vector3(x = 1.0) else up
            val upward = forward.crossProduct(safeUp.crossProduct(forward)).normaliseCopy()
            val leftward = safeUp.crossProduct(forward).normaliseCopy()

            return Mat3(arrayOf(
                    doubleArrayOf(forward.x, leftward.x, upward.x),
                    doubleArrayOf(forward.y, leftward.y, upward.y),
                    doubleArrayOf(forward.z, leftward.z, upward.z)))
        }
    }
}
