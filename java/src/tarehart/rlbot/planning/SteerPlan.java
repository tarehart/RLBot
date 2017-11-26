package tarehart.rlbot.planning;

import tarehart.rlbot.math.Circle;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;

import java.awt.*;
import java.awt.geom.Line2D;

public class SteerPlan {

    public AgentOutput immediateSteer;
    public Vector2 waypoint;
    public Circle circle;

    public SteerPlan(AgentOutput immediateSteer, Vector2 waypoint) {
        this(immediateSteer, waypoint, null);
    }

    public SteerPlan(AgentOutput immediateSteer, Vector2 waypoint, Circle circle) {
        this.immediateSteer = immediateSteer;
        this.waypoint = waypoint;
        this.circle = circle;
    }

    public SteerPlan(CarData car, Vector3 position) {
        this.immediateSteer = SteerUtil.steerTowardGroundPosition(car, position);
        this.waypoint = position.flatten();
    }

    public void drawDebugInfo(Graphics2D graphics, CarData car) {
        graphics.setStroke(new BasicStroke(1));
        if (circle != null) {
            graphics.draw(circle.toShape());
        }
        graphics.draw(new Line2D.Double(car.position.x, car.position.y, waypoint.x, waypoint.y));
    }
}
