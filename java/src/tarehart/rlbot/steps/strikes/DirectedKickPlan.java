package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.ui.ArenaDisplay;

import java.awt.*;
import java.awt.geom.Line2D;

public class DirectedKickPlan {
    public Intercept intercept;
    public BallPath ballPath;
    public DistancePlot distancePlot;
    public BallSlice ballAtIntercept;
    public Vector3 interceptModifier;
    public Vector3 desiredBallVelocity;
    public Vector3 plannedKickForce;
    public Vector2 launchPad;

    public void drawDebugInfo(Graphics2D graphics) {
        graphics.setColor(new Color(73, 111, 73));
        ArenaDisplay.drawBall(ballAtIntercept.space, graphics, graphics.getColor());
        graphics.setStroke(new BasicStroke(1));

        Vector2 carAtOffset = intercept.getSpace().flatten();
        int crossSize = 2;
        graphics.draw(new Line2D.Double(carAtOffset.getX() - crossSize, carAtOffset.getY() - crossSize, carAtOffset.getX() + crossSize, carAtOffset.getY() + crossSize));
        graphics.draw(new Line2D.Double(carAtOffset.getX() - crossSize, carAtOffset.getY() + crossSize, carAtOffset.getX() + crossSize, carAtOffset.getY() - crossSize));
    }
}
