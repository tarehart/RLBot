package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import kotlin.math.cos
import kotlin.math.sin

object VectorUtil {

    fun project(vector: Vector3, onto: Vector3): Vector3 {
        val scale = vector.dotProduct(onto) / onto.magnitudeSquared()
        return onto.scaled(scale)
    }

    fun project(vector: Vector2, onto: Vector2): Vector2 {
        val scale = vector.dotProduct(onto) / onto.magnitudeSquared()
        return onto.scaled(scale)
    }

    fun flatDistance(a: Vector3, b: Vector3): Float {
        return a.flatten().distance(b.flatten())
    }

    fun flatDistance(a: Vector3, b: Vector3, planeNormal: Vector3): Float {
        return a.projectToPlane(planeNormal).distance(b.projectToPlane(planeNormal))
    }

    fun getPlaneIntersection(plane: Plane, segmentPosition: Vector3, segmentVector: Vector3): Vector3? {
        // get d value
        val d = plane.normal.dotProduct(plane.position)

        if (plane.normal.dotProduct(segmentVector) == 0F) {
            return null // No intersection, the line is parallel to the plane
        }

        // Compute the X value for the directed line ray intersecting the plane
        val x = (d - plane.normal.dotProduct(segmentPosition)) / plane.normal.dotProduct(segmentVector)

        if (x < 0) {
            return null; // This means the 'intersection' is in the opposite direction.
        }

        // output contact point
        val intersection = segmentPosition.plus(segmentVector.scaled(x))

        return if (intersection.distance(segmentPosition) > segmentVector.magnitude()) {
            null
        } else {
            intersection
        }

    }

    fun rotateVector(vec: Vector2, radians: Number): Vector2 {
        val r = radians.toFloat()
        return Vector2(
                vec.x * cos(r) - vec.y * sin(r),
                vec.x * sin(r) + vec.y * cos(r))
    }

    fun orthogonal(vec: Vector2): Vector2 {
        return Vector2(vec.y, -vec.x)
    }

    fun orthogonal(vec: Vector2, left: Boolean): Vector2 {
        // Start: (0, 1)
        // Left would be: (-1, 0)
        return if (left) Vector2(-vec.y, vec.x) else Vector2(vec.y, -vec.x)
    }

    /**
     * There are two possible orthogonal vectors. We will use the isCorrectDirection function to determine
     * which should be returned. isCorrectDirection takes in a candidate orthogonal vector and returns
     * true if it looks good.
     */
    fun orthogonal(vec: Vector2, isCorrectDirection: (Vector2) -> Boolean): Vector2 {
        val result = orthogonal(vec)
        return if (isCorrectDirection.invoke(result)) {
            result
        } else result.scaled(-1F)
    }

    fun getCorrectionAngle(current: Vector3, ideal: Vector3, up: Vector3): Float {

        val currentProj = current.projectToPlane(up)
        val idealProj = ideal.projectToPlane(up)
        var angle = currentProj.angle(idealProj)

        val cross = currentProj.crossProduct(idealProj)

        if (cross.dotProduct(up) < 0) {
            angle *= -1F
        }
        return angle
    }

    fun weightedAverage(a: Vector3, b: Vector3, weightOfA: Float): Vector3 {
        val weightedA = a.scaled(weightOfA)
        val weightedB = b.scaled(1 - weightOfA)
        return weightedA.plus(weightedB)
    }
}
