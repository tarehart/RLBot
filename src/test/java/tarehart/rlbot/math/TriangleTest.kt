package tarehart.rlbot.math

import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

class TriangleTest {

    @Test
    fun testSSA() {

        val knownAngle = Math.PI.toFloat() / 6
        val knownSide = 10F
        val partialSide = 8F
        val t1 = Triangle.sideSideAngle(knownSide, partialSide, knownAngle)
        assertNotNull(t1)

        if (t1 == null) {
            return
        }

        val delta = 0.01F
        assertEquals(Math.PI.toFloat(), t1.angleA + t1.angleB + t1.angleC, delta)

        assertEquals(knownAngle, t1.angleA, delta)
        assertEquals(0.412F, t1.angleB, delta)
        assertEquals(2.206F, t1.angleC, delta)
        assertEquals(knownSide, t1.sideA, delta)
        assertEquals(partialSide, t1.sideB, delta)
        assertEquals(16.093F, t1.sideC, delta)
    }

}
