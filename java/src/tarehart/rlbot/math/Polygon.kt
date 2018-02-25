package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector2

import java.awt.geom.Area
import java.awt.geom.Path2D

class Polygon(points: Array<Vector2>) {

    private val area: Area

    // copy for safety
    val awtArea: Area
        get() = Area(area)

    init {

        if (points.size < 3) {
            throw IllegalArgumentException("Field area must have at least 3 points!")
        }

        val path = Path2D.Double()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }

        area = Area(path)
    }

    operator fun contains(test: Vector2): Boolean {
        return area.contains(test.x, test.y)
    }
}
