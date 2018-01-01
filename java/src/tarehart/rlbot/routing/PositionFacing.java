package tarehart.rlbot.routing;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.ui.ArenaDisplay;

import java.awt.*;

public class PositionFacing {

    private final Vector2 position;
    private final Vector2 facing;

    public PositionFacing(Vector2 position, Vector2 facing) {
        this.position = position;
        this.facing = facing;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getFacing() {
        return facing;
    }

    public void drawDebugInfo(Graphics2D graphics) {
        graphics.setColor(Color.green);
        ArenaDisplay.drawCar(this, 0, graphics);
    }
}
