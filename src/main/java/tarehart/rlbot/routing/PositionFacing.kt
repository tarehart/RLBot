package tarehart.rlbot.routing

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.ui.ArenaDisplay

import java.awt.*

class PositionFacing(val position: Vector2, val facing: Vector2) {

    fun drawDebugInfo(graphics: Graphics2D) {
        graphics.color = Color.green
        ArenaDisplay.drawCar(this, 0.0, graphics)
    }
}
