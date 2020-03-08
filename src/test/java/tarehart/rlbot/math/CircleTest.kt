package tarehart.rlbot.math

import org.junit.Test

import org.junit.Assert.*
import tarehart.rlbot.math.vector.Vector2
import kotlin.math.atan
import kotlin.math.sin
import kotlin.math.sqrt

class CircleTest {

    @Test
    fun calculateTangentPointsWithSlopeIdealized() {
        val c = Circle(Vector2(0, 0), 1)
        val tangentPoints = c.calculateTangentPointsWithSlope(Vector2(1, 1))
        assertEquals(sqrt(2F) / 2, tangentPoints.first.x)
        assertEquals(-sqrt(2F) / 2, tangentPoints.first.y)
        assertEquals(-sqrt(2F) / 2, tangentPoints.second.x)
        assertEquals(sqrt(2F) / 2, tangentPoints.second.y)
    }

    @Test
    fun calculateTangentPointsWithSlopeTrickyRatio() {
        val c = Circle(Vector2(0, 0), 1)
        val tangentPoints = c.calculateTangentPointsWithSlope(Vector2(1, 2))
        assertEquals(sin(atan(0.5F)), tangentPoints.second.y)
    }

    @Test
    fun calculateTangentPointsWithSlopeStrangeRadius() {
        val c = Circle(Vector2(0, 0), 4)
        val tangentPoints = c.calculateTangentPointsWithSlope(Vector2(1, 1))
        assertEquals(sqrt(2F) * 2, tangentPoints.first.x)
        assertEquals(-sqrt(2F) * 2, tangentPoints.first.y)
        assertEquals(-sqrt(2F) * 2, tangentPoints.second.x)
        assertEquals(sqrt(2F) * 2, tangentPoints.second.y)
    }

    @Test
    fun calculateTangentPointsWithSlopeTranslated() {
        val c = Circle(Vector2(2, 8), 4)
        val tangentPoints = c.calculateTangentPointsWithSlope(Vector2(1, 1))
        assertEquals(sqrt(2F) * 2 + 2, tangentPoints.first.x)
        assertEquals(-sqrt(2F) * 2 + 8, tangentPoints.first.y)
        assertEquals(-sqrt(2F) * 2 + 2, tangentPoints.second.x)
        assertEquals(sqrt(2F) * 2 + 8, tangentPoints.second.y)
    }

    @Test
    fun calculateTangentPointsWithSlopeInfinite() {
        val c = Circle(Vector2(0, 0), 1)
        val tangentPoints = c.calculateTangentPointsWithSlope(Vector2(1, 0))
        assertEquals(1F, tangentPoints.first.y)
    }

    @Test
    fun calculateTangentPointsWithSlopeZero() {
        val c = Circle(Vector2(0, 0), 1)
        val tangentPoints = c.calculateTangentPointsWithSlope(Vector2(0, 1))
        assertEquals(1F, tangentPoints.first.x)
    }
}
