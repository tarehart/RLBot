package tarehart.rlbot.math;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;

import java.util.List;

// NOTE: Behavior in this class is not guaranteed for non-convex polygons
public class Polygon {

    // List of points in the order they are connected
    public Vector2[] points;


    public Polygon(Vector2[] points) {
        this.points = points;
    }


    //NOTE: 3d polygons are not supported yet, so the input is essentially flattened
    public boolean contains(Vector3 test) {
        return contains(new Vector2(test.x, test.y));
    }

    // Counts the number of edge intersects in one direction of the y axis from the
    // test point through the polygon's edges
    public boolean contains(Vector2 test) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.length - 1; i < points.length; j = i++) {
            if ((points[i].y > test.y) != (points[j].y > test.y) &&
                    (test.x < (points[j].x - points[i].x) * (test.y - points[i].y)
                    / (points[j].y-points[i].y) + points[i].x)) {
                result = !result; // Only return true if the total number of intersects is odd
            }
        }
        return result;
    }
}
