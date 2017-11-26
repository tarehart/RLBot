package tarehart.rlbot.planning;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.tuning.ManeuverMath;

public class AirTouchPlanner {

    private static final double AERIAL_RISE_RATE = 8;
    public static final double BOOST_NEEDED_FOR_AERIAL = 20;
    public static final double NEEDS_AERIAL_THRESHOLD = ManeuverMath.MASH_JUMP_HEIGHT;
    public static final double MAX_JUMP_HIT = NEEDS_AERIAL_THRESHOLD;
    public static final double NEEDS_JUMP_HIT_THRESHOLD = 3.6;
    public static final double NEEDS_FRONT_FLIP_THRESHOLD = 2.2;
    public static final double CAR_BASE_HEIGHT = ManeuverMath.BASE_CAR_Z;
    private static final double MAX_FLIP_HIT = NEEDS_JUMP_HIT_THRESHOLD;


    public static AerialChecklist checkAerialReadiness(CarData car, SpaceTime carPositionAtContact) {

        AerialChecklist checklist = new AerialChecklist();
        checkLaunchReadiness(checklist, car, carPositionAtContact);

        checklist.notSkidding = car.velocity.normaliseCopy().dotProduct(car.orientation.noseVector) > .99;
        checklist.hasBoost = car.boost >= BOOST_NEEDED_FOR_AERIAL;

        return checklist;
    }

    public static LaunchChecklist checkJumpHitReadiness(CarData car, SpaceTime carPositionAtContact) {

        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, car, carPositionAtContact);
        return checklist;
    }

    public static LaunchChecklist checkFlipHitReadiness(CarData car, SpaceTime intercept) {
        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, car, intercept);
        checklist.notTooClose = true;
        checklist.timeForIgnition = TimeUtil.secondsBetween(car.time, intercept.time) < InterceptStep.FLIP_HIT_STRIKE_PROFILE.speedupSeconds;
        return checklist;
    }

    private static void checkLaunchReadiness(LaunchChecklist checklist, CarData car, SpaceTime carPositionAtContact) {

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, carPositionAtContact.space);
        double secondsTillIntercept = TimeUtil.secondsBetween(car.time, carPositionAtContact.time);
        double tMinus = getAerialLaunchCountdown(carPositionAtContact.space.z, secondsTillIntercept);

        checklist.linedUp = Math.abs(correctionAngleRad) < Math.PI / 60;
        checklist.closeEnough = secondsTillIntercept < 4;
        checklist.notTooClose = isVerticallyAccessible(car, carPositionAtContact);
        checklist.timeForIgnition = tMinus < 0.1;
        checklist.upright = car.orientation.roofVector.dotProduct(new Vector3(0, 0, 1)) > .99;
        checklist.onTheGround = car.position.z < CAR_BASE_HEIGHT + 0.03; // Add a little wiggle room
    }

    public static boolean isVerticallyAccessible(CarData carData, SpaceTime intercept) {
        double secondsTillIntercept = TimeUtil.secondsBetween(carData.time, intercept.time);

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

        double secondsTillIntercept = TimeUtil.secondsBetween(carData.time, intercept.time);
        double tMinus = getJumpLaunchCountdown(intercept.space.z, secondsTillIntercept);
        return tMinus >= -0.1;
    }

    public static boolean isJumpSideFlipAccessible(CarData carData, SpaceTime intercept) {
        if (intercept.space.z > ManeuverMath.MASH_JUMP_HEIGHT) {
            return false;
        }

        double secondsTillIntercept = TimeUtil.secondsBetween(carData.time, intercept.time);
        double tMinus = getJumpLaunchCountdown(intercept.space.z, secondsTillIntercept);
        return tMinus >= -0.1;
    }

    public static boolean isFlipHitAccessible(CarData carData, SpaceTime intercept) {
        return intercept.space.z <= MAX_FLIP_HIT;
    }

    private static double getAerialLaunchCountdown(double height, double secondsTillIntercept) {
        double expectedAerialSeconds = (height - CAR_BASE_HEIGHT) / AERIAL_RISE_RATE;
        return secondsTillIntercept - expectedAerialSeconds;
    }

    private static double getJumpLaunchCountdown(double height, double secondsTillIntercept) {
        double expectedJumpSeconds = ManeuverMath.secondsForMashJumpHeight(height).orElse(Double.MAX_VALUE);
        return secondsTillIntercept - expectedJumpSeconds;
    }

    public static double getBoostBudget(CarData carData) {
        return carData.boost - BOOST_NEEDED_FOR_AERIAL - 5;
    }
}
