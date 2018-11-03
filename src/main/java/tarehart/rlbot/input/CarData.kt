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
        val renderer: Renderer
)
