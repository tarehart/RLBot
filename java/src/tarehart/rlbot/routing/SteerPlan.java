package tarehart.rlbot.routing;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Circle;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.routing.PositionFacing;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

public class SteerPlan {

    public AgentOutput immediateSteer;
    public Vector2 waypoint;
    private PositionFacing target;
    public Circle circle;
    public boolean clockwise;

    public SteerPlan(AgentOutput immediateSteer, Vector2 waypoint) {
        this(immediateSteer, waypoint, null, null, null, false);
    }

    public SteerPlan(AgentOutput immediateSteer, Vector2 waypoint, Vector2 targetPosition, Vector2 targetFacing, Circle circle, boolean clockwise) {
        this.immediateSteer = immediateSteer;
        this.waypoint = waypoint;
        this.target = new PositionFacing(targetPosition, targetFacing);
        this.circle = circle;
        this.clockwise = clockwise;
    }

    public void drawDebugInfo(Graphics2D graphics, CarData car) {

        graphics.setStroke(new BasicStroke(1));
        graphics.draw(new Line2D.Double(car.getPosition().getX(), car.getPosition().getY(), waypoint.getX(), waypoint.getY()));
        if (circle != null) {
            Shape circleShape = circle.toShape();


            //graphics.draw(circleShape);

            Vector2 centerToWaypoint = waypoint.minus(circle.getCenter());
            Vector2 centerToFinal = target.getPosition().minus(circle.getCenter());
            double waypointAngle = Math.atan2(centerToWaypoint.getY(), centerToWaypoint.getX());
            double waypointDegrees = -waypointAngle * 180 / Math.PI;
            double radians = centerToWaypoint.correctionAngle(centerToFinal, clockwise);
            double extent = -radians * 180 / Math.PI;

            Arc2D.Double arc = new Arc2D.Double(circleShape.getBounds2D(), waypointDegrees, extent, Arc2D.OPEN);
            graphics.draw(arc);

            graphics.draw(new Circle(waypoint, 1).toShape());

            target.drawDebugInfo(graphics);
        }

    }
}
