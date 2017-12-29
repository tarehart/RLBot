package tarehart.rlbot.planning;

import tarehart.rlbot.math.Plane;
import tarehart.rlbot.math.Polygon;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;


public class Goal {

    private static final double GOAL_DISTANCE = 102;
    public static final double GOAL_HEIGHT = 12;
    public static final double EXTENT = 17.8555;

    private Vector3 center;
    private Plane threatPlane;
    private Plane scorePlane;
    private Polygon box;

    public Goal(boolean negativeSide) {

        center = new Vector3(0, GOAL_DISTANCE * (negativeSide ? -1 : 1), 0);

        threatPlane = new Plane(new Vector3(0, negativeSide ? 1 : -1, 0), new Vector3(0, (GOAL_DISTANCE - 1) * (negativeSide ? -1 : 1), 0));
        scorePlane = new Plane(new Vector3(0, negativeSide ? 1 : -1, 0), new Vector3(0, (GOAL_DISTANCE + 2) * (negativeSide ? -1 : 1), 0));
        box = negativeSide ? ZoneDefinitions.BLUEBOX : ZoneDefinitions.ORANGEBOX;
    }


    public Vector3 getNearestEntrance(Vector3 ballPosition, double padding) {

        double adjustedExtent = EXTENT - ArenaModel.BALL_RADIUS - padding;
        double adjustedHeight = GOAL_HEIGHT - ArenaModel.BALL_RADIUS - padding;
        double x = Math.min(adjustedExtent, Math.max(-adjustedExtent, ballPosition.getX()));
        double z = Math.min(adjustedHeight, Math.max(ArenaModel.BALL_RADIUS, ballPosition.getZ()));
        return new Vector3(x, center.getY(), z);
    }

    public Plane getThreatPlane() {
        return threatPlane;
    }

    public Plane getScorePlane() {
        return scorePlane;
    }

    public Vector3 getCenter() {
        return center;
    }

    /**
     * From shooter's perspective
     */
    public Vector3 getLeftPost() {
        return getLeftPost(0);
    }

    /**
     * From shooter's perspective
     */
    public Vector3 getLeftPost(double padding) {
        return new Vector3(center.getX() - (EXTENT - padding) * Math.signum(center.getY()), center.getY(), center.getZ());
    }

    /**
     * From shooter's perspective
     */
    public Vector3 getRightPost() {
        return getRightPost(0);
    }

    /**
     * From shooter's perspective
     */
    public Vector3 getRightPost(double padding) {
        return new Vector3(center.getX() + (EXTENT - padding) * Math.signum(center.getY()), center.getY(), center.getZ());
    }

    public boolean isInBox(Vector3 position) {
        return box.contains(position.flatten());
    }
}
