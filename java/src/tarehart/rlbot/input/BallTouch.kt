package tarehart.rlbot.input

import tarehart.rlbot.Bot
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

class BallTouch(val team: Bot.Team, val playerIndex: Int, val time: GameTime, val position: Vector3, val normal: Vector3)
