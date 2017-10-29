package tarehart.rlbot.physics;

public class BallPhysics {
    public static double getGroundBounceEnergy(double height, double verticalVelocity) {
        double potentialEnergy = (height - ArenaModel.BALL_RADIUS) * ArenaModel.GRAVITY;
        double verticalKineticEnergy = 0.5 * verticalVelocity * verticalVelocity;
        return potentialEnergy + verticalKineticEnergy;
    }
}
