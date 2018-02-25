package tarehart.rlbot.math

import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*
import tarehart.rlbot.math.vector.Vector3

class PlaneTest {
    @Test
    fun distance() {

        val p = Plane(Vector3(0.0, 0.0, 1.0), Vector3(5.0, 7.0, 3.0))
        val point = Vector3(-13.4, 5.6, 13.0)
        val distance = p.distance(point)

        Assert.assertEquals(10.0, distance, 0.0)
    }

}