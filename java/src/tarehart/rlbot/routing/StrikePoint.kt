package tarehart.rlbot.routing

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.GameTime

class StrikePoint(val position: Vector2, facing: Vector2, val expectedTime: GameTime, val waitUntil: GameTime? = null) {

    val facing: Vector2 = facing.normalized()
    val positionFacing = PositionFacing(position, facing)
}