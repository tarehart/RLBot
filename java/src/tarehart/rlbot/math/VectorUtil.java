package tarehart.rlbot.math;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;

import java.util.Optional;
import java.util.function.Function;

public class VectorUtil {

    public static Vector3 project(Vector3 vector, Vector3 onto) {
        double scale = vector.dotProduct(onto) / onto.magnitudeSquared();
        return onto.scaled(scale);
    }

    public static Vector2 project(Vector2 vector, Vector2 onto) {
        double scale = vector.dotProduct(onto) / onto.magnitudeSquared();
        return onto.scaled(scale);
    }

    public static double flatDistance(Vector3 a, Vector3 b) {
        return a.flatten().distance(b.flatten());
    }

    public static double flatDistance(Vector3 a, Vector3 b, Vector3 planeNormal) {
        return a.projectToPlane(planeNormal).distance(b.projectToPlane(planeNormal));
    }

    public static Optional<Vector3> getPlaneIntersection(Plane plane, Vector3 segmentPosition, Vector3 segmentVector) {
        // get d value
        double d = plane.normal.dotProduct(plane.position);

        if (plane.normal.dotProduct(segmentVector) == 0) {
            return Optional.empty(); // No intersection, the line is parallel to the plane
        }

        // Compute the X value for the directed line ray intersecting the plane
        double x = (d - plane.normal.dotProduct(segmentPosition)) / plane.normal.dotProduct(segmentVector);

        // output contact point
        Vector3 intersection = segmentPosition.plus(segmentVector.scaled(x));

        if (intersection.distance(segmentPosition) > segmentVector.magnitude()) {
            return Optional.empty();
        }

        return Optional.of(intersection);
    }

    public static Vector2 rotateVector(Vector2 vec, double radians) {
        return new Vector2(
                vec.getX() * Math.cos(radians) - vec.getY() * Math.sin(radians),
                vec.getX() * Math.sin(radians) + vec.getY() * Math.cos(radians));
    }

    public static Vector2 orthogonal(Vector2 vec) {
        return new Vector2(vec.getY(), -vec.getX());
    }

    /**
     * There are two possible orthogonal vectors. We will use the isCorrectDirection function to determine
     * which should be returned. isCorrectDirection takes in a candidate orthogonal vector and returns
     * true if it looks good.
     */
    public static Vector2 orthogonal(Vector2 vec, Function<Vector2, Boolean> isCorrectDirection) {
        Vector2 result = orthogonal(vec);
        if (isCorrectDirection.apply(result)) {
            return result;
        }
        return result.scaled(-1);
    }

    public static double getCorrectionAngle(Vector3 current, Vector3 ideal, Vector3 up) {

        Vector3 currentProj = current.projectToPlane(up);
        Vector3 idealProj = ideal.projectToPlane(up);
        double angle = currentProj.angle(idealProj);

        Vector3 cross = currentProj.crossProduct(idealProj);

        if (cross.dotProduct(up) < 0) {
            angle *= -1;
        }
        return angle;
    }
}
