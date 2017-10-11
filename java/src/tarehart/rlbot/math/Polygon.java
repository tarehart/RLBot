package tarehart.rlbot.math;

import tarehart.rlbot.math.vector.Vector2;

import java.awt.geom.Area;
import java.awt.geom.Path2D;

public class Polygon {

    private final Area area;

    public Polygon(Vector2[] points) {

        if (points.length < 3) {
            throw new IllegalArgumentException("Field area must have at least 3 points!");
        }

        Path2D.Double path = new Path2D.Double();
        path.moveTo(points[0].x, points[0].y);
        for (int i = 1; i < points.length; i++) {
            path.lineTo(points[i].x, points[i].y);
        }

        area = new Area(path);
    }

    public boolean contains(Vector2 test) {
        return area.contains(test.x, test.y);
    }

    public Area getAwtArea() {
        return area;
    }
}
