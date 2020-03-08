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

        assertEquals(10F, distance, 0F)
    }

    @Test
    fun intersect() {
        val ground = Plane(Vector3.UP, Vector3())
        val wall = Plane(Vector3(x = 1.0), Vector3(x = -8.0))

        val intersect = ground.intersect(wall)
        if (intersect == null) {
            fail()
            return
        }
        assertEquals(0F, intersect.direction.x, 0F)
        assertEquals(0F, intersect.direction.z, 0F)
        assertEquals(1F, Math.abs(intersect.direction.y), 0F)

        assertEquals(-8F, intersect.position.x, 0F)
        assertEquals(0F, intersect.position.z, 0F)
    }

}
