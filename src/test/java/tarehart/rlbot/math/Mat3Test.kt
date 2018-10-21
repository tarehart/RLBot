package tarehart.rlbot.math

import org.junit.Assert
import org.junit.Test

import tarehart.rlbot.math.vector.Vector3

class Mat3Test {

    val identity = Mat3(arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
    ))

    val turnRight = Mat3(arrayOf(
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(-1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
    ))

    @Test
    fun dot() {

        val vec = Vector3(1.0, 2.0, 3.0)

        // The identity matrix should not change anything
        Assert.assertEquals(vec, identity.dot(vec))

        Assert.assertEquals(Vector3(0.0, -1.0, 0.0), turnRight.dot(Vector3(1.0, 0.0, 0.0)))
        Assert.assertEquals(Vector3(-1.0, 0.0, 0.0), turnRight.dot(Vector3(0.0, -1.0, 0.0)))

        // The transpose of turn right should turn back to the left
        Assert.assertEquals(Vector3(0.0, -1.0, 0.0), turnRight.transpose().dot(Vector3(-1.0, 0.0, 0.0)))

    }

}
