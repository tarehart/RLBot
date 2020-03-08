package tarehart.rlbot.math.vector

import org.junit.Assert
import org.junit.Test
import tarehart.rlbot.math.BotMath.PI

class Vector2Test {

    @Test
    fun correctionAngle() {
        val delta = 0.001F
        Assert.assertEquals(PI / 4, Vector2(1.0, 0.0).correctionAngle(Vector2(1.0, 1.0)), delta)
        Assert.assertEquals(-PI / 4, Vector2(0.0, 1.0).correctionAngle(Vector2(1.0, 1.0)), delta)
        Assert.assertEquals(3 * PI / 4, Vector2(0.0, -1.0).correctionAngle(Vector2(1.0, 1.0)), delta)
    }
}
