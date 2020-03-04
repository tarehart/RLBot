package tarehart.rlbot.math

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rlbot.render.NamedRenderer
import rlbot.render.Renderer
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.input.CarSpin
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime
import java.lang.Math.cos
import java.lang.Math.sin

class OrientationSolverTest {

    private val renderer = NamedRenderer("test")

    @Before
    fun setup() {
        renderer.startPacket()
    }

    @After
    fun teardown() {
        renderer.finishPacket()
    }


    @Test
    fun step_shouldPitchUp_whenCarPointingDown() {

        val tilt = Math.PI / 6

        val orientation = CarOrientation(Vector3(cos(tilt), 0.0, -sin(tilt)), Vector3(sin(tilt), 0.0, cos(tilt)))
        val car = makeCarData(orientation)

        val agentOutput = OrientationSolver.orientCar(car, Mat3.lookingTo(Vector3(x = 1.0)), 1 / 60.0)

        val epsilon = 0.0001
        Assert.assertTrue(agentOutput.pitch > 0.5) // Doesn't go full force because we need to slow down soon
        Assert.assertEquals(0.0, agentOutput.yaw, epsilon)
        Assert.assertEquals(0.0, agentOutput.roll, epsilon)

    }

    @Test
    fun step_shouldRollLeft_whenCarRolledRight() {

        val tilt = Math.PI / 6

        val orientation = CarOrientation(Vector3(1.0, 0.0, 0.0), Vector3(0.0, -sin(tilt), cos(tilt)))
        val car = makeCarData(orientation)

        val agentOutput = OrientationSolver.orientCar(car, Mat3.lookingTo(Vector3(x = 1.0)), 1 / 60.0)

        val epsilon = 0.0001
        Assert.assertTrue(agentOutput.roll < 0) // Doesn't go full force because we need to slow down soon
        Assert.assertEquals(0.0, agentOutput.pitch, epsilon)
        Assert.assertEquals(0.0, agentOutput.yaw, epsilon)

    }

    @Test
    fun step_shouldYawLeft_whenCarYawedRight() {

        val orientation = CarOrientation(Vector3(0.0, -1.0, 0.0), Vector3.UP)
        val car = makeCarData(orientation)

        val agentOutput = OrientationSolver.orientCar(car, Mat3.lookingTo(Vector3(x = 1.0)), 1 / 60.0)

        val epsilon = 0.0001
        Assert.assertTrue(agentOutput.yaw < 0)
        Assert.assertEquals(0.0, agentOutput.pitch, epsilon)
        Assert.assertEquals(0.0, agentOutput.roll, epsilon)

    }

    private fun makeCarData(orientation: CarOrientation): CarData {
        val car = CarData(
                position = Vector3(),
                velocity = Vector3(),
                orientation = orientation,
                spin = CarSpin(Vector3(), orientation.matrix),
                boost = 100.0,
                isSupersonic = false,
                team = Team.BLUE,
                playerIndex = 0,
                time = GameTime(0),
                frameCount = 0,
                hasWheelContact = false,
                isDemolished = false,
                name = "testCar",
                renderer = renderer,
                isBot = true)
        return car
    }
}
