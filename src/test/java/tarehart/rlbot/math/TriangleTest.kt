package tarehart.rlbot.math

import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

class TriangleTest {

    @Test
    fun testSSA() {

        val knownAngle = Math.PI / 6
        val knownSide = 10.0
        val partialSide = 8.0
        val t1 = Triangle.sideSideAngle(knownSide, partialSide, knownAngle)
        Assert.assertNotNull(t1)

        if (t1 == null) {
            return
        }

        val delta = 0.01
        Assert.assertEquals(Math.PI, t1.angleA + t1.angleB + t1.angleC, delta)

        Assert.assertEquals(knownAngle, t1.angleA, delta)
        Assert.assertEquals(0.412, t1.angleB, delta)
        Assert.assertEquals(2.206, t1.angleC, delta)
        Assert.assertEquals(knownSide, t1.sideA, delta)
        Assert.assertEquals(partialSide, t1.sideB, delta)
        Assert.assertEquals(16.093, t1.sideC, delta)
    }

}