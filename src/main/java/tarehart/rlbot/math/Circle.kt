package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import java.awt.Shape
import java.awt.geom.Ellipse2D
import kotlin.math.abs
import kotlin.math.sqrt

class Circle(val center: Vector2, val radius: Float) {

    constructor(center: Vector2, radius: Number): this(center, radius.toFloat())

    fun toShape(): Shape {
        return Ellipse2D.Float(center.x - radius, center.y - radius, radius * 2, radius * 2)
    }

    // https://stackoverflow.com/questions/3349125/circle-circle-intersection-points
    fun intersect(other: Circle): Pair<Vector2, Vector2>? {

        val d = this.center.distance(other.center)

        if (d >= this.radius + other.radius) {
            return null
        }

        if (d < abs(this.radius - other.radius)) {
            return null
        }

        val a = (radius * radius - other.radius * other.radius + d * d) / (2 * d)
        val h = sqrt(radius * radius - a * a)
        val p2 = (other.center - this.center).scaled(a / d) + this.center

        return Pair(
                Vector2(p2.x + h*(other.center.y - this.center.y)/d,
                        p2.y - h*(other.center.x - this.center.x)/d),
                Vector2(p2.x - h*(other.center.y - this.center.y)/d,
                        p2.y + h*(other.center.x - this.center.x)/d))
    }

    fun calculateTangentPointsToExternalPoint(outsidePoint: Vector2): Pair<Vector2, Vector2>? {
        val toMidpoint = (center - outsidePoint).scaled(.5F)
        val midpoint = outsidePoint + toMidpoint

        val thalesCircle = Circle(midpoint, toMidpoint.magnitude())
        return intersect(thalesCircle)
    }

    /**
     * Given a slope, find the two tangent points on this circle which have that slope.
     * Could probably implement this with atan also.
     */
    fun calculateTangentPointsWithSlope(slopeVector: Vector2): Pair<Vector2, Vector2> {
        if (slopeVector.y == 0F) {
            return Pair(Vector2(0, radius) + center, Vector2(0, -radius) + center)
        }

        val ratio = -slopeVector.x / slopeVector.y
        val x1 = radius * sqrt(1 / (ratio * ratio + 1))
        val x2 = -x1
        val y1 = x1 * ratio
        val y2 = x2 * ratio
        return Pair(Vector2(x1, y1) + center, Vector2(x2, y2) + center)
    }

    companion object {

        // https://stackoverflow.com/questions/4103405/what-is-the-algorithm-for-finding-the-center-of-a-circle-from-three-points
        fun getCircleFromPoints(a: Vector2, b: Vector2, c: Vector2): Circle {
            val yDelta1 = b.y - a.y
            var xDelta1 = b.x - a.x
            val yDelta2 = c.y - b.y
            var xDelta2 = c.x - b.x

            if (xDelta1 == 0F) {
                xDelta1 = .00001F
            }

            if (xDelta2 == 0F) {
                xDelta2 = .00001F
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

        fun getCircleFromSphereSlice(center: Vector3, radius: Float, sliceHeight: Float): Circle? {
            val diff = Math.abs(center.z - sliceHeight)
            val capHeight = radius - diff

            if (capHeight <= 0) {
                return null
            }

            val sliceRadius = sqrt(radius * 2 * capHeight - capHeight * capHeight)

            return Circle(center.flatten(), sliceRadius)
        }
    }
}
