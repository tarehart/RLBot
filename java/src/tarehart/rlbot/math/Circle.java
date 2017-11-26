package tarehart.rlbot.math;

import tarehart.rlbot.math.vector.Vector2;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Circle {

    public Vector2 center;
    public double radius;

    public Circle(Vector2 center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    // https://stackoverflow.com/questions/4103405/what-is-the-algorithm-for-finding-the-center-of-a-circle-from-three-points
    public static Circle getCircleFromPoints(Vector2 a, Vector2 b, Vector2 c) {
        double yDelta1 = b.y - a.y;
        double xDelta1 = b.x - a.x;
        double yDelta2 = c.y - b.y;
        double xDelta2 = c.x - b.x;

        if (xDelta1 == 0) {
            xDelta1 = .00001;
        }

        if (xDelta2 == 0) {
            xDelta2 = .00001;
        }

        double slope1 = yDelta1/xDelta1;
        double slope2 = yDelta2/xDelta2;
        double cx = (slope1*slope2*(a.y - c.y) + slope2*(a.x + b.x)
                - slope1*(b.x+c.x) )/(2* (slope2-slope1) );
        double cy = -1*(cx - (a.x+b.x)/2)/slope1 +  (a.y+b.y)/2;
        Vector2 center = new Vector2(cx,cy);
        return new Circle(center, center.distance(a));
    }

    public static boolean isClockwise(Circle circle, Vector2 tangentPosition, Vector2 tangentDirection) {
        Vector2 tangentToCenter = tangentPosition.minus(circle.center);
        return tangentToCenter.correctionAngle(tangentDirection) < 0;
    }

    public Shape toShape() {
        return new Ellipse2D.Double(center.x - radius, center.y - radius, radius * 2, radius * 2);
    }
}
