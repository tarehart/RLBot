package tarehart.rlbot.routing

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.ui.ArenaDisplay

import java.awt.*


class PositionFacing(val position: Vector2, facing: Vector2) {

    constructor(car: CarData) : this(car.position.flatten(), car.orientation.noseVector.flatten())

    val facing = facing.normalized()

    fun drawDebugInfo(graphics: Graphics2D) {
        graphics.color = Color.green
        ArenaDisplay.drawCar(this, 0.0, graphics)
    }

    fun distanceRemaining(entity: Vector2): Double {
        val toPosition = position - entity
        return toPosition.dotProduct(facing) / facing.magnitudeSquared()
    }
}
