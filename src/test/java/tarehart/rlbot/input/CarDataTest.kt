package tarehart.rlbot.input

import org.junit.Assert
import org.junit.Test
import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.rendering.NullRenderer
import tarehart.rlbot.time.GameTime

class CarDataTest {

    @Test
    fun relativePosition_showsForward() {
        val car = makeCar(Vector3(0.0, 0.0, 0.0), CarOrientation(Vector3(0.0, 1.0, 0.0), Vector3.UP))
        val relativePosition = car.relativePosition(Vector3(0.0, 10.0, 0.0))
        Assert.assertEquals(10.0, relativePosition.x, 0.001)
    }

    @Test
    fun relativePosition_showsLeft() {
        val car = makeCar(Vector3(0.0, 0.0, 0.0), CarOrientation(Vector3(0.0, 1.0, 0.0), Vector3.UP))
        val relativePosition = car.relativePosition(Vector3(20.0, 0.0, 0.0))
        Assert.assertEquals(-20.0, relativePosition.y, 0.001)
    }

    @Test
    fun relativePosition_showsUp() {
        val car = makeCar(Vector3(0.0, 0.0, 0.0), CarOrientation(Vector3(0.0, 1.0, 0.0), Vector3.UP))
        val relativePosition = car.relativePosition(Vector3(0.0, 0.0, 10.0))
        Assert.assertEquals(10.0, relativePosition.z, 0.001)
    }

    @Test
    fun relativePosition_considersCarPosition() {
        val car = makeCar(Vector3(5.0, 5.0, 5.0), CarOrientation(Vector3(0.0, 1.0, 0.0), Vector3.UP))
        val relativePosition = car.relativePosition(Vector3(10.0, 10.0, 20.0))
        Assert.assertEquals(5.0, relativePosition.x, 0.001)
        Assert.assertEquals(-5.0, relativePosition.y, 0.001)
        Assert.assertEquals(15.0, relativePosition.z, 0.001)
    }

    private fun makeCar(position: Vector3, orientation: CarOrientation): CarData {
        return CarData(
                position,
                Vector3.ZERO,
                orientation,
                CarSpin(0.0, 0.0, 0.0, Vector3.UP),
                10.0, false, Team.BLUE, 0, GameTime.zero(), 0,
                true, false, "Gaucho", NullRenderer())
    }
}
