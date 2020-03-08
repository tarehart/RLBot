package tarehart.rlbot.physics


import org.junit.Assert
import org.junit.Test
import tarehart.rlbot.math.vector.Vector3

class ArenaModelTest {

    @Test
    fun testConstruct() {
        val model = ArenaModel()
    }

    @Test
    fun testNearestPlane() {

        val delta = 0F

        val groundPlane = ArenaModel.getNearestPlane(Vector3(0.0, 0.0, 0.0))
        Assert.assertEquals(1F, groundPlane.normal.z, delta)

        val sideWall = ArenaModel.getNearestPlane(Vector3(ArenaModel.SIDE_WALL - 10.0, 30.0, 16.0))
        Assert.assertEquals(-1F, sideWall.normal.x, delta)

        val ceiling = ArenaModel.getNearestPlane(Vector3(16.0, 30.0, ArenaModel.CEILING - 10.0))
        Assert.assertEquals(-1F, ceiling.normal.z, delta)
    }

    @Test
    fun testBouncePlane() {
        val bouncePlane = ArenaModel.getBouncePlane(Vector3(0.0, 0.0, 5.0), Vector3(1.0, 0.0, 0.0))
        Assert.assertEquals(-1F, bouncePlane.normal.x, 0F)
    }

}
