package tarehart.rlbot.math

import org.ejml.data.FMatrixRMaj
import org.ejml.dense.row.CommonOps_FDRM
import tarehart.rlbot.math.vector.Vector3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


class Mat3(private val matrix: FMatrixRMaj) {

    constructor(values: Array<FloatArray>): this(FMatrixRMaj(values))

    fun transpose(): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.transpose(matrix, ret)
        return Mat3(ret)
    }

    fun dot(vec: Vector3): Vector3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.mult(matrix, toMatrix(vec), ret)
        return toVec(ret)
    }

    fun dot(mat: Mat3): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.mult(matrix, mat.matrix, ret)
        return Mat3(ret)
    }

    fun trace(): Float {
        return CommonOps_FDRM.trace(matrix)
    }

    fun get(row: Int, col: Int): Float {
        return matrix.get(row, col)
    }

    operator fun plus(mat: Mat3): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.add(matrix, mat.matrix, ret)
        return Mat3(ret)
    }

    operator fun times(value: Float): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.scale(value, matrix, ret)
        return Mat3(ret)
    }

    operator fun times(other: Mat3): Mat3 {
        val ret = emptyMatrix()
        CommonOps_FDRM.mult(matrix, other.matrix, ret)
        return Mat3(ret)
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
        val IDENTITY = Mat3(FMatrixRMaj(arrayOf(floatArrayOf(1F, 0F, 0F), floatArrayOf(0F, 1F, 0F), floatArrayOf(0F, 0F, 1F))))

        private fun emptyMatrix() = FMatrixRMaj(3, 3)

        private fun toMatrix(vec: Vector3): FMatrixRMaj {
            return FMatrixRMaj(arrayOf(floatArrayOf(vec.x), floatArrayOf(vec.y), floatArrayOf(vec.z)))
        }

        private fun toVec(matrix: FMatrixRMaj): Vector3 {
            return Vector3(matrix.get(0), matrix.get(1), matrix.get(2))
        }

        fun lookingTo(direction: Vector3, up: Vector3 = Vector3.UP): Mat3 {
            val forward = direction.normaliseCopy()
            val safeUp = if (abs(forward.z) == 1F && abs(up.z) == 1F) Vector3(x = 1.0) else up
            val upward = forward.crossProduct(safeUp.crossProduct(forward)).normaliseCopy()
            val leftward = safeUp.crossProduct(forward).normaliseCopy()

            return Mat3(arrayOf(
                    floatArrayOf(forward.x, leftward.x, upward.x),
                    floatArrayOf(forward.y, leftward.y, upward.y),
                    floatArrayOf(forward.z, leftward.z, upward.z)))
        }

        fun rotationMatrix(unitAxis: Vector3, rad: Double): Mat3 {
            val cosTheta = cos(rad).toFloat()
            val sinTheta = sin(rad).toFloat()
            val n1CosTheta = 1F - cosTheta
            val u = unitAxis
            return Mat3(arrayOf(
                    floatArrayOf(cosTheta + u.x * u.x * n1CosTheta, u.x * u.y * n1CosTheta - u.z * sinTheta, u.x * u.z * n1CosTheta + u.y * sinTheta),
                    floatArrayOf(u.y * u.x * n1CosTheta + u.z * sinTheta, cosTheta + u.y * u.y * n1CosTheta, u.y * u.z * n1CosTheta - u.x * sinTheta),
                    floatArrayOf(u.z * u.x * n1CosTheta - u.y * sinTheta, u.z * u.y * n1CosTheta + u.x * sinTheta, cosTheta + u.z * u.z * n1CosTheta)
            ))
        }
    }
}
