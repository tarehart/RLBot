package tarehart.rlbot.math

import org.ejml.simple.SimpleMatrix
import tarehart.rlbot.math.vector.Vector3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


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

    operator fun times(matrix: Mat3): Mat3 {
        return Mat3(this.matrix.mult(matrix.matrix))
    }

    fun forward(): Vector3 {
        return Vector3(
                this.matrix[0, 0],
                this.matrix[1, 0],
                this.matrix[2, 0]
        )
    }

    fun left(): Vector3 {
        return Vector3(
                this.matrix[0, 1],
                this.matrix[1, 1],
                this.matrix[2, 1]
        )
    }

    fun up(): Vector3 {
        return Vector3(
                this.matrix[0, 2],
                this.matrix[1, 2],
                this.matrix[2, 2]
        )
    }

    /**
     * https://github.com/samuelpmish/RLUtilities/blob/f071b4dec24d3389f21727ca2d95b75980cbb5fb/RLUtilities/cpp/inc/linalg.h#L71-L74
     */
    fun angleTo(other: Mat3): Double {
        val dot = this.dot(other.transpose())
        val trace = dot.trace()
        return acos(0.5 * (trace - 1))
    }

    /* XXX: Broken/untested
    fun rotationToVector(): Vector3 {
        // There are some cases where this will not work and I don't fully understand the eigenvalues.
        return Vector3(
                this.matrix[2, 1] - this.matrix[1, 2],
                this.matrix[0, 2] - this.matrix[2, 0],
                this.matrix[1, 0] - this.matrix[0, 1]
        ).normaliseCopy()
    }
    */

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

        fun rotationMatrix(unitAxis: Vector3, rad: Double): Mat3 {
            val cosTheta = cos(rad)
            val sinTheta = sin(rad)
            val n1CosTheta = 1.0 - cosTheta
            val u = unitAxis
            return Mat3(arrayOf(
                    doubleArrayOf(cosTheta + u.x * u.x * n1CosTheta, u.x * u.y * n1CosTheta - u.z * sinTheta, u.x * u.z * n1CosTheta + u.y * sinTheta),
                    doubleArrayOf(u.y * u.x * n1CosTheta + u.z * sinTheta, cosTheta + u.y * u.y * n1CosTheta, u.y * u.z * n1CosTheta - u.x * sinTheta),
                    doubleArrayOf(u.z * u.x * n1CosTheta - u.y * sinTheta, u.z * u.y * n1CosTheta + u.x * sinTheta, cosTheta + u.z * u.z * n1CosTheta)
            ))
        }
    }
}
