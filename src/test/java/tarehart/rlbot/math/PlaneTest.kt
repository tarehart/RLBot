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

    @Test
    fun intersect() {
        val ground = Plane(Vector3.UP, Vector3())
        val wall = Plane(Vector3(x = 1.0), Vector3(x = -8.0))

        val intersect = ground.intersect(wall)
        if (intersect == null) {
            Assert.fail()
            return
        }
        Assert.assertEquals(0.0, intersect.direction.x, 0.0)
        Assert.assertEquals(0.0, intersect.direction.z, 0.0)
        Assert.assertEquals(1.0, Math.abs(intersect.direction.y), 0.0)

        Assert.assertEquals(-8.0, intersect.position.x, 0.0)
        Assert.assertEquals(0.0, intersect.position.z, 0.0)
    }

}
