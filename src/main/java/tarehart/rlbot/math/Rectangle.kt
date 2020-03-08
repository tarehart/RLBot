package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector2
import java.awt.geom.Rectangle2D

class Rectangle(val awtRectangle: Rectangle2D) {

    operator fun contains(test: Vector2): Boolean {
        return awtRectangle.contains(test.x.toDouble(), test.y.toDouble())
    }
}
