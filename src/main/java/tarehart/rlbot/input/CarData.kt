package tarehart.rlbot.input

import rlbot.render.Renderer
import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

class CarData (
        val position: Vector3,
        val velocity: Vector3,
        val orientation: CarOrientation,
        val spin: CarSpin,
        val boost: Double,
        val isSupersonic: Boolean,
        val team: Team,
        val playerIndex: Int,
        val time: GameTime,
        val frameCount: Long,
        val hasWheelContact: Boolean,
        val isDemolished: Boolean,
        val name: String,
        val renderer: Renderer,
        val isBot: Boolean
) {

    /**
     * x is distance forward of the car.
     * y is distance left of the car.
     * z is distance above the car.
     */
    fun relativePosition(target: Vector3): Vector3 {
        val toTarget = target - position
        return orientation.matrix.transpose().dot(toTarget)
    }
}
