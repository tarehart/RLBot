package tarehart.rlbot.intercept;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.SteerUtil;
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
        double secondsTillIntercept = Duration.Companion.between(car.getTime(), intercept.getTime()).getSeconds();
        double tMinus = getAerialLaunchCountdown(intercept.getSpace().getZ(), secondsTillIntercept);
        checklist.timeForIgnition = tMinus < 0.1;
        checklist.notSkidding = !ManeuverMath.isSkidding(car);
        checklist.hasBoost = car.getBoost() >= BOOST_NEEDED_FOR_AERIAL;

        return checklist;
    }

    public static LaunchChecklist checkJumpHitReadiness(CarData car, Intercept intercept) {

        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, car, intercept);
        StrikeProfile strikeProfile = intercept.getStrikeProfile();
        double jumpHitTime = strikeProfile.getStrikeDuration().getSeconds();
        checklist.timeForIgnition = Duration.Companion.between(car.getTime(), intercept.getTime()).getSeconds() < jumpHitTime;
        return checklist;
    }

    public static LaunchChecklist checkFlipHitReadiness(CarData car, Intercept intercept) {
        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, car, intercept);
        checklist.timeForIgnition = Duration.Companion.between(car.getTime(), intercept.getTime()).getSeconds() < intercept.getStrikeProfile().getStrikeDuration().getSeconds();
        return checklist;
    }

    private static void checkLaunchReadiness(LaunchChecklist checklist, CarData car, Intercept intercept) {

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, intercept.getSpace());
        double secondsTillIntercept = Duration.Companion.between(car.getTime(), intercept.getTime()).getSeconds();

        checklist.linedUp = Math.abs(correctionAngleRad) < Math.PI / 30;
        checklist.closeEnough = secondsTillIntercept < 4;

        checklist.upright = car.getOrientation().getRoofVector().dotProduct(new Vector3(0, 0, 1)) > .99;
        checklist.onTheGround = car.getHasWheelContact();
    }

    public static boolean isVerticallyAccessible(CarData carData, SpaceTime intercept) {
        double secondsTillIntercept = Duration.Companion.between(carData.getTime(), intercept.getTime()).getSeconds();

        if (intercept.getSpace().getZ() < NEEDS_AERIAL_THRESHOLD) {
            double tMinus = secondsTillIntercept - getJumpHitStrikeProfile(intercept.getSpace()).getStrikeDuration().getSeconds();
            return tMinus >= -0.1;
        }

        if (carData.getBoost() > BOOST_NEEDED_FOR_AERIAL) {
            double tMinus = getAerialLaunchCountdown(intercept.getSpace().getZ(), secondsTillIntercept);
            return tMinus >= -0.1;
        }
        return false;
    }

    public static boolean isJumpHitAccessible(CarData carData, SpaceTime intercept) {
        if (intercept.getSpace().getZ() > MAX_JUMP_HIT) {
            return false;
        }

        double secondsTillIntercept = Duration.Companion.between(carData.getTime(), intercept.getTime()).getSeconds();
        StrikeProfile jumpHitStrikeProfile = getJumpHitStrikeProfile(intercept.getSpace());
        double tMinus = secondsTillIntercept - jumpHitStrikeProfile.maneuverSeconds;
        return tMinus >= -0.1;
    }

    public static StrikeProfile getJumpHitStrikeProfile(Vector3 space) {
        // If we have time to tilt back, the nose will be higher and we can cheat a little.
        double requiredHeight = space.getZ() * .7;
        double jumpTime = ManeuverMath.secondsForMashJumpHeight(requiredHeight).orElse(.8);
        return new StrikeProfile(0, jumpTime, 10, .15, StrikeProfile.Style.JUMP_HIT);
    }

    public static boolean isFlipHitAccessible(CarData carData, SpaceTime intercept) {
        return intercept.getSpace().getZ() <= MAX_FLIP_HIT;
    }

    private static double getAerialLaunchCountdown(double height, double secondsTillIntercept) {
        double expectedAerialSeconds = (height - CAR_BASE_HEIGHT) / AERIAL_RISE_RATE;
        return secondsTillIntercept - expectedAerialSeconds;
    }

    public static double expectedSecondsForSuperJump(double height) {
        return (height - CAR_BASE_HEIGHT) / SUPER_JUMP_RISE_RATE;
    }

    public static double getBoostBudget(CarData carData) {
        return carData.getBoost() - BOOST_NEEDED_FOR_AERIAL - 5;
    }
}
