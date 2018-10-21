package tarehart.rlbot.math

import org.ejml.simple.SimpleMatrix
import tarehart.rlbot.math.vector.Vector3


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

    companion object {
        private fun toMatrix(vec: Vector3): SimpleMatrix {
            return SimpleMatrix(arrayOf(doubleArrayOf(vec.x), doubleArrayOf(vec.y), doubleArrayOf(vec.z)))
        }

        private fun toVec(matrix: SimpleMatrix): Vector3 {
            return Vector3(matrix.get(0), matrix.get(1), matrix.get(2))
        }

        fun lookingTo(direction: Vector3, up: Vector3 = Vector3.UP): Mat3 {
            val forward = direction.normaliseCopy()
            val upward = forward.crossProduct(up.crossProduct(forward)).normaliseCopy()
            val leftward = up.crossProduct(forward).normaliseCopy()

            return Mat3(arrayOf(
                    doubleArrayOf(forward.x, leftward.x, upward.x),
                    doubleArrayOf(forward.y, leftward.y, upward.y),
                    doubleArrayOf(forward.z, leftward.z, upward.z)))
        }

        fun dot(vec: Vector3, mat: Mat3): Vector3 {
            return toVec(toMatrix(vec).mult(mat.matrix))
        }
    }
}
