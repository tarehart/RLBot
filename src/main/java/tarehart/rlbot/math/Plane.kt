package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector3
import java.util.*

class Plane(normal: Vector3, position: Vector3): Ray(position, normal) {

    val normal
        get() = this.direction

    private val constant
        get() = normal.dotProduct(position)

    /**
     * This is direction sensitive. Distance will be negative if the point is
     * behind the plane!
     */
    fun distance(point: Vector3): Double {
        return (point - this.position).dotProduct(this.normal)
    }

    fun projectPoint(point: Vector3): Vector3 {
        return point - normal.scaled(distance(point))
    }

    /**
     * Code taken from https://stackoverflow.com/a/38437831
     *
     * Algorithm taken from http://geomalgorithms.com/a05-_intersect-1.html. See the
     * section 'Intersection of 2 Planes' and specifically the subsection
     * (A) Direct Linear Equation
     */
    fun intersect(p2: Plane): Ray? {

        // the cross product gives us the direction of the line at the intersection
        // of the two planes, and gives us an easy way to check if the two planes
        // are parallel - the cross product will have zero magnitude
        val direction = normal.crossProduct(p2.normal)
        if (direction.isZero) {
            return null
        }

        // now find a point on the intersection. We use the 'Direct Linear Equation'
        // method described in the linked page, and we choose which coordinate
        // to set as zero by seeing which has the largest absolute value in the
        // directional vector

        val x = Math.abs(direction.x)
        val y = Math.abs(direction.y)
        val z = Math.abs(direction.z)

        val point: Vector3

        point = if (z >= x && z >= y) {
            solveIntersectingPoint(2, 0, 1, this, p2)
        } else if (y >= z && y >= x){
            solveIntersectingPoint(1, 2, 0, this, p2)
        } else {
            solveIntersectingPoint(0, 1, 2, this, p2)
        }

        return Ray(point, direction)
    }

    /**
     * This method helps finding a point on the intersection between two planes.
     * Depending on the orientation of the planes, the problem could solve for the
     * zero point on either the x, y or z axis
     */
    private fun solveIntersectingPoint(zeroIndex: Int, aIndex: Int, bIndex: Int, p1: Plane, p2: Plane): Vector3 {
        val a1 = p1.normal[aIndex]
        val b1 = p1.normal[bIndex]
        val d1 = p1.constant

        val a2 = p2.normal[aIndex]
        val b2 = p2.normal[bIndex]
        val d2 = p2.constant

        val a0 = ((b2 * d1) - (b1 * d2)) / ((a1 * b2 - a2 * b1))
        val b0 = ((a1 * d2) - (a2 * d1)) / ((a1 * b2 - a2 * b1))

        val components = LinkedList<Pair<Int, Double>>()
        components.add(Pair(zeroIndex, 0.0))
        components.add(Pair(aIndex, a0))
        components.add(Pair(bIndex, b0))

        components.sortBy { pair -> pair.first }

        return Vector3(components[0].second, components[1].second, components[2].second)
    }
}
