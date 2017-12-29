package tarehart.rlbot.input

import tarehart.rlbot.Bot
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

class CarData (
    val position: Vector3,
    val velocity: Vector3,
    val orientation: CarOrientation,
    val spin: CarSpin,
    val boost: Double,
    val isSupersonic: Boolean,
    val team: Bot.Team,
    val playerIndex: Int,
    val time: GameTime,
    val frameCount: Long,
    val hasWheelContact: Boolean,
    val isDemolished: Boolean
)
