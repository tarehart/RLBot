package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector2

import java.awt.*
import java.awt.geom.Ellipse2D

class Circle(val center: Vector2, val radius: Double) {

    fun toShape(): Shape {
        return Ellipse2D.Double(center.x - radius, center.y - radius, radius * 2, radius * 2)
    }

    companion object {

        // https://stackoverflow.com/questions/4103405/what-is-the-algorithm-for-finding-the-center-of-a-circle-from-three-points
        fun getCircleFromPoints(a: Vector2, b: Vector2, c: Vector2): Circle {
            val yDelta1 = b.y - a.y
            var xDelta1 = b.x - a.x
            val yDelta2 = c.y - b.y
            var xDelta2 = c.x - b.x

            if (xDelta1 == 0.0) {
                xDelta1 = .00001
            }

            if (xDelta2 == 0.0) {
                xDelta2 = .00001
            }

            val slope1 = yDelta1 / xDelta1
            val slope2 = yDelta2 / xDelta2
            val cx = (slope1 * slope2 * (a.y - c.y) + slope2 * (a.x + b.x) - slope1 * (b.x + c.x)) / (2 * (slope2 - slope1))
            val cy = -1 * (cx - (a.x + b.x) / 2) / slope1 + (a.y + b.y) / 2
            val center = Vector2(cx, cy)
            return Circle(center, center.distance(a))
        }

        fun isClockwise(circle: Circle, tangentPosition: Vector2, tangentDirection: Vector2): Boolean {
            val tangentToCenter = tangentPosition.minus(circle.center)
            return tangentToCenter.correctionAngle(tangentDirection) < 0
        }
    }
}
