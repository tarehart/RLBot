package tarehart.rlbot.input

import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

data class BoostPad(val location: Vector3, val isFullBoost: Boolean, var isActive: Boolean = false, var activeTime: GameTime = GameTime.zero())