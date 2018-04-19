package tarehart.rlbot.routing

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.time.GameTime


class StrikePoint(val position: Vector2, val facing: Vector2, val gameTime: GameTime) {
    val positionFacing: PositionFacing
        get() = PositionFacing(position, facing)
}