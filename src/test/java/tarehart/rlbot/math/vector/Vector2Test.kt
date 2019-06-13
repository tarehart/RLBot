package tarehart.rlbot.math.vector

import org.junit.Assert
import org.junit.Test

class Vector2Test {

    @Test
    fun correctionAngle() {
        Assert.assertEquals(Math.PI / 4, Vector2(1.0, 0.0).correctionAngle(Vector2(1.0, 1.0)), 0.001)
        Assert.assertEquals(-Math.PI / 4, Vector2(0.0, 1.0).correctionAngle(Vector2(1.0, 1.0)), 0.001)
        Assert.assertEquals(3 * Math.PI / 4, Vector2(0.0, -1.0).correctionAngle(Vector2(1.0, 1.0)), 0.001)
    }
}
