package tarehart.rlbot.planning;

import tarehart.rlbot.math.vector.Vector2;

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
}
