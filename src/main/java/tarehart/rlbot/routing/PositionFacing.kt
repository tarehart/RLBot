package tarehart.rlbot.routing

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.ui.ArenaDisplay
import java.awt.Color
import java.awt.Graphics2D


class PositionFacing(val position: Vector2, facing: Vector2) {

    constructor(car: CarData) : this(car.position.flatten(), car.orientation.noseVector.flatten())

    val facing = facing.normalized()

    fun drawDebugInfo(graphics: Graphics2D) {
        graphics.color = Color.green
        ArenaDisplay.drawCar(this, 0F, graphics)
    }

    fun distanceRemaining(entity: Vector2): Float {
        val toPosition = position - entity
        return toPosition.dotProduct(facing) / facing.magnitudeSquared()
    }
}
