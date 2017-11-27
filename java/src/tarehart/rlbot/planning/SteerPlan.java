package tarehart.rlbot.planning;

import tarehart.rlbot.math.Circle;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;

public class SteerPlan {

    public AgentOutput immediateSteer;
    public Vector2 waypoint;
    private Vector2 targetPosition;
    private Vector2 targetFacing;
    public Circle circle;

    public SteerPlan(AgentOutput immediateSteer, Vector2 waypoint) {
        this(immediateSteer, waypoint, null, null, null);
    }

    public SteerPlan(AgentOutput immediateSteer, Vector2 waypoint, Vector2 targetPosition, Vector2 targetFacing, Circle circle) {
        this.immediateSteer = immediateSteer;
        this.waypoint = waypoint;
        this.targetPosition = targetPosition;
        this.targetFacing = targetFacing;
        this.circle = circle;
    }

    public SteerPlan(CarData car, Vector3 position) {
        this.immediateSteer = SteerUtil.steerTowardGroundPosition(car, position);
        this.waypoint = position.flatten();
    }

    public void drawDebugInfo(Graphics2D graphics, CarData car) {



        graphics.setStroke(new BasicStroke(1));
        graphics.draw(new Line2D.Double(car.position.x, car.position.y, waypoint.x, waypoint.y));
        if (circle != null) {
            Shape circleShape = circle.toShape();


            //graphics.draw(circleShape);

            Vector2 centerToWaypoint = waypoint.minus(circle.center);
            Vector2 centerToFinal = targetPosition.minus(circle.center);
            double waypointAngle = Math.atan2(centerToWaypoint.y, centerToWaypoint.x);
            double waypointDegrees = -waypointAngle * 180 / Math.PI;
            double radians = centerToWaypoint.correctionAngle(centerToFinal);
            double extent = -radians * 180 / Math.PI;

            Arc2D.Double arc = new Arc2D.Double(circleShape.getBounds2D(), waypointDegrees, extent, Arc2D.OPEN);
            graphics.draw(arc);
        }

    }
}
