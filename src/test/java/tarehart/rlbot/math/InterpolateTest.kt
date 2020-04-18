package tarehart.rlbot.math

import org.junit.Test

import org.junit.Assert.*

class InterpolateTest {

    @Test
    fun getValue() {
        assertEquals(.5F, Interpolate.getValue(listOf(Pair(0F, 0F), Pair(1F, 1F)), .5F))
        assertEquals(0F, Interpolate.getValue(listOf(Pair(0F, 0F), Pair(1F, 1F)), 0F))
        assertEquals(1F, Interpolate.getValue(listOf(Pair(0F, 0F), Pair(1F, 1F)), 1F))
        assertEquals(4.5F, Interpolate.getValue(listOf(Pair(0F, 0F), Pair(1F, 1F), Pair(2F, 8F)), 1.5F))
        assertNull(Interpolate.getValue(listOf(Pair(0F, 0F), Pair(1F, 1F), Pair(2F, 8F)), 3F))
        assertNull(Interpolate.getValue(listOf(Pair(0F, 0F), Pair(1F, 1F), Pair(2F, 8F)), -1F))
    }

    @Test
    fun getInverse() {
        assertEquals(.5F, Interpolate.getInverse(listOf(Pair(0F, 0F), Pair(1F, 1F)), .5F))
        assertEquals(0F, Interpolate.getInverse(listOf(Pair(0F, 0F), Pair(1F, 1F)), 0F))
        assertEquals(1F, Interpolate.getInverse(listOf(Pair(0F, 0F), Pair(1F, 1F)), 1F))
        assertEquals(1.5F, Interpolate.getInverse(listOf(Pair(0F, 0F), Pair(1F, 1F), Pair(2F, 8F)), 4.5F))
        assertEquals(-2.5F, Interpolate.getInverse(listOf(Pair(-4F, -1F), Pair(-3F, -4F), Pair(-2F, 4F)), 0F))
        assertEquals(-3F, Interpolate.getInverse(listOf(Pair(-4F, -1F), Pair(-3F, -5F), Pair(-2F, 8F)), -5F))
    }

    @Test
    fun isBetween() {
        assertTrue(Interpolate.isBetween(5F, 6F, 3F))
        assertTrue(Interpolate.isBetween(5F, 1F, 10F))
        assertTrue(Interpolate.isBetween(5F, 5F, 5F))
        assertFalse(Interpolate.isBetween(1F, 2F, 3F))
    }
}
