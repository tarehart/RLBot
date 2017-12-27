package tarehart.rlbot.planning;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.tuning.ManeuverMath;

public class AirTouchPlanner {

    private static final double AERIAL_RISE_RATE = 10;
    private static final double SUPER_JUMP_RISE_RATE = 11;
    public static final double BOOST_NEEDED_FOR_AERIAL = 20;
    public static final double NEEDS_AERIAL_THRESHOLD = ManeuverMath.MASH_JUMP_HEIGHT;
    public static final double MAX_JUMP_HIT = NEEDS_AERIAL_THRESHOLD;
    public static final double NEEDS_JUMP_HIT_THRESHOLD = 3.6;
    public static final double NEEDS_FRONT_FLIP_THRESHOLD = 2.2;
    public static final double CAR_BASE_HEIGHT = ManeuverMath.BASE_CAR_Z;
    private static final double MAX_FLIP_HIT = NEEDS_JUMP_HIT_THRESHOLD;


    public static AerialChecklist checkAerialReadiness(CarData car, Intercept intercept) {

        AerialChecklist checklist = new AerialChecklist();
        checkLaunchReadiness(checklist, car, intercept);

        checklist.notSkidding = car.velocity.isZero() || car.velocity.normaliseCopy().dotProduct(car.orientation.noseVector) > .99;
        checklist.hasBoost = car.boost >= BOOST_NEEDED_FOR_AERIAL;

        return checklist;
    }

    public static LaunchChecklist checkJumpHitReadiness(CarData car, Intercept intercept) {

        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, car, intercept);
        StrikeProfile strikeProfile = intercept.getStrikeProfile();
        double jumpHitTime = strikeProfile.getTotalDuration().getSeconds();
        checklist.timeForIgnition = Duration.between(car.time, intercept.getTime()).getSeconds() < jumpHitTime;
        return checklist;
    }

    public static LaunchChecklist checkFlipHitReadiness(CarData car, Intercept intercept) {
        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, car, intercept);
        checklist.notTooClose = true;
        checklist.timeForIgnition = Duration.between(car.time, intercept.getTime()).getSeconds() < intercept.getStrikeProfile().getTotalDuration().getSeconds();
        return checklist;
    }

    private static void checkLaunchReadiness(LaunchChecklist checklist, CarData car, Intercept intercept) {

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, intercept.getSpace());
        double secondsTillIntercept = Duration.between(car.time, intercept.getTime()).getSeconds();
        double tMinus = getAerialLaunchCountdown(intercept.getSpace().z, secondsTillIntercept);

        checklist.linedUp = Math.abs(correctionAngleRad) < Math.PI / 60;
        checklist.closeEnough = secondsTillIntercept < 4;
        checklist.notTooClose = isVerticallyAccessible(car, intercept.toSpaceTime());
        checklist.timeForIgnition = tMinus < 0.1;
        checklist.upright = car.orientation.roofVector.dotProduct(new Vector3(0, 0, 1)) > .99;
        checklist.onTheGround = car.hasWheelContact;
    }

    public static boolean isVerticallyAccessible(CarData carData, SpaceTime intercept) {
        double secondsTillIntercept = Duration.between(carData.time, intercept.time).getSeconds();

        if (intercept.space.z < NEEDS_AERIAL_THRESHOLD) {
            double tMinus = getJumpLaunchCountdown(intercept.space.z, secondsTillIntercept);
            return tMinus >= -0.1;
        }

        if (carData.boost > BOOST_NEEDED_FOR_AERIAL) {
            double tMinus = getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept);
            return tMinus >= -0.1;
        }
        return false;
    }

    public static boolean isJumpHitAccessible(CarData carData, SpaceTime intercept) {
        if (intercept.space.z > MAX_JUMP_HIT) {
            return false;
        }

        double secondsTillIntercept = Duration.between(carData.time, intercept.time).getSeconds();
        StrikeProfile jumpHitStrikeProfile = getJumpHitStrikeProfile(intercept.space);
        double tMinus = secondsTillIntercept - jumpHitStrikeProfile.maneuverSeconds;
        return tMinus >= -0.1;
    }

    public static StrikeProfile getJumpHitStrikeProfile(Vector3 space) {
        // If we have time to tilt back, the nose will be higher and we can cheat a little.
        double requiredHeight = space.z * .7;
        double jumpTime = ManeuverMath.secondsForMashJumpHeight(requiredHeight).orElse(.8);
        return new StrikeProfile(jumpTime, 10, .15, StrikeProfile.Style.JUMP_HIT);
    }

    public static boolean isFlipHitAccessible(CarData carData, SpaceTime intercept) {
        return intercept.space.z <= MAX_FLIP_HIT;
    }

    private static double getAerialLaunchCountdown(double height, double secondsTillIntercept) {
        double expectedAerialSeconds = (height - CAR_BASE_HEIGHT) / AERIAL_RISE_RATE;
        return secondsTillIntercept - expectedAerialSeconds;
    }

    public static double expectedSecondsForSuperJump(double height) {
        return (height - CAR_BASE_HEIGHT) / SUPER_JUMP_RISE_RATE;
    }

    private static double getJumpLaunchCountdown(double height, double secondsTillIntercept) {
        double expectedJumpSeconds = ManeuverMath.secondsForMashJumpHeight(height).orElse(Double.MAX_VALUE);
        return secondsTillIntercept - expectedJumpSeconds;
    }

    public static double getBoostBudget(CarData carData) {
        return carData.boost - BOOST_NEEDED_FOR_AERIAL - 5;
    }
}
