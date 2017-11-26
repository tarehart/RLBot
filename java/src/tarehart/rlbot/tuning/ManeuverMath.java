package tarehart.rlbot.tuning;

import java.util.Optional;

public class ManeuverMath {

    public static final double DODGE_SPEED = 10;

    public static final double BASE_CAR_Z = 0.3405;

    public static final double MASH_JUMP_HEIGHT = 4.8; // This is absolute height, so subtract BASE_CAR_Z if you want relative height.

    private static final double TAP_JUMP_HEIGHT = 1.88 - BASE_CAR_Z;
    private static final double TAP_JUMP_APEX_TIME = 0.5;


    private static final double MASH_A = -6.451144;
    private static final double MASH_B = 11.26689;
    private static final double MASH_C = -0.09036379;
    private static final double MASH_SLOPE = 7.78;
    private static final double SLOPE_CUTOFF = 3.2;

    public static Optional<Double> secondsForMashJumpHeight(double height) {
        if (height > MASH_JUMP_HEIGHT) {
            return Optional.empty();
        }

        if (height < SLOPE_CUTOFF) {
            return Optional.of((height - BASE_CAR_Z) / MASH_SLOPE);
        }

        double d = MASH_B * MASH_B -4 * MASH_A * (MASH_C - height);
        if (d < 0){
            return Optional.empty(); // Too high!
        }
        return Optional.of((-MASH_B + Math.sqrt(d)) / (2 * MASH_A));
    }

    public static double secondsForSideFlipTravel(double distance) {
        return distance / DODGE_SPEED;
    }

}
