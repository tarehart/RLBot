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
        val groundPlane = ArenaModel.getNearestPlane(Vector3(0.0, 0.0, 0.0))
        Assert.assertEquals(1.0, groundPlane.normal.z, 0.0)

        val sideWall = ArenaModel.getNearestPlane(Vector3(ArenaModel.SIDE_WALL - 10.0, 30.0, 16.0))
        Assert.assertEquals(-1.0, sideWall.normal.x, 0.0)

        val ceiling = ArenaModel.getNearestPlane(Vector3(16.0, 30.0, ArenaModel.CEILING - 10.0))
        Assert.assertEquals(-1.0, ceiling.normal.z, 0.0)
    }

    @Test
    fun testBouncePlane() {
        val bouncePlane = ArenaModel.getBouncePlane(Vector3(0.0, 0.0, 5.0), Vector3(1.0, 0.0, 0.0))
        Assert.assertEquals(-1.0, bouncePlane.normal.x, 0.0)
    }

}
