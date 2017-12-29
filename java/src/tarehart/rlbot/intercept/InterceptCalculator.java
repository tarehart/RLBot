package tarehart.rlbot.intercept;

import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.steps.strikes.MidairStrikeStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class InterceptCalculator {

    public static Optional<Intercept> getInterceptOpportunityAssumingMaxAccel(CarData carData, BallPath ballPath, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), boostBudget);

        return getInterceptOpportunity(carData, ballPath, plot);
    }

    public static Optional<Intercept> getInterceptOpportunity(CarData carData, BallPath ballPath, DistancePlot acceleration) {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, new Vector3(), (a, b) -> true);
    }

    public static Optional<Intercept> getFilteredInterceptOpportunity(
            CarData carData, BallPath ballPath, DistancePlot acceleration, Vector3 interceptModifier, BiPredicate<CarData, SpaceTime> predicate) {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, interceptModifier, predicate, (space) -> new StrikeProfile());
    }

    public static Optional<Intercept> getFilteredInterceptOpportunity(
            CarData carData,
            BallPath ballPath,
            DistancePlot acceleration,
            Vector3 interceptModifier,
            BiPredicate<CarData, SpaceTime> predicate,
            Function<Vector3, StrikeProfile> strikeProfileFn) {

        Vector3 groundNormal = new Vector3(0, 0, 1);
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, interceptModifier, predicate, strikeProfileFn, groundNormal);
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param acceleration
     * @param interceptModifier an offset from the ball position that the car is trying to reach
     * @param predicate determines whether a particular ball slice is eligible for intercept
     * @param strikeProfileFn a description of how the car will move during the final moments of the intercept
     * @param planeNormal the normal of the plane that the car is driving on for this intercept.
     * @return
     */
    public static Optional<Intercept> getFilteredInterceptOpportunity(
            CarData carData,
            BallPath ballPath,
            DistancePlot acceleration,
            Vector3 interceptModifier,
            BiPredicate<CarData, SpaceTime> predicate,
            Function<Vector3, StrikeProfile> strikeProfileFn,
            Vector3 planeNormal) {

        Vector3 myPosition = carData.getPosition();
        GameTime firstMomentInRange = null;

        for (BallSlice ballMoment: ballPath.getSlices()) {
            SpaceTime spaceTime = new SpaceTime(ballMoment.getSpace().plus(interceptModifier), ballMoment.getTime());
            StrikeProfile strikeProfile = strikeProfileFn.apply(spaceTime.space);
            Optional<DistanceTimeSpeed> motionAt = acceleration.getMotionAfterDuration(carData, spaceTime.space, Duration.between(carData.getTime(), spaceTime.time), strikeProfile);
            if (motionAt.isPresent()) {
                DistanceTimeSpeed dts = motionAt.get();
                double interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space, planeNormal);
                if (dts.getDistance() > interceptDistance) {
                    if (firstMomentInRange == null) {
                        firstMomentInRange = spaceTime.time;
                    }
                    if (predicate.test(carData, spaceTime)) {
                        double boostNeeded = spaceTime.space.getZ() > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD ? AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL : 0;
                        Duration spareTime = Duration.between(firstMomentInRange, spaceTime.time);
                        return Optional.of(new Intercept(spaceTime.space, spaceTime.time, boostNeeded, strikeProfile, acceleration, spareTime));
                    }
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param interceptModifier an offset from the ball position that the car is trying to reach
     * @return
     */
    public static Optional<SpaceTime> getAerialIntercept(
            CarData carData,
            BallPath ballPath,
            Vector3 interceptModifier) {

        Vector3 myPosition = carData.getPosition();

        for (BallSlice ballMoment: ballPath.getSlices()) {
            SpaceTime intercept = new SpaceTime(ballMoment.getSpace().plus(interceptModifier), ballMoment.getTime());

            double averageNoseAngle = MidairStrikeStep.getDesiredVerticalAngle(carData.getVelocity(), intercept.space.minus(carData.getPosition()));
            Duration duration = Duration.between(carData.getTime(), ballMoment.getTime());
            DistancePlot acceleration = AccelerationModel.simulateAirAcceleration(carData, duration, Math.cos(averageNoseAngle));
            StrikeProfile strikeProfile = duration.compareTo(MidairStrikeStep.MAX_TIME_FOR_AIR_DODGE) < 0 && averageNoseAngle < .5 ?
                    new StrikeProfile(0, 10, .15, StrikeProfile.Style.AERIAL) :
                    InterceptStep.AERIAL_STRIKE_PROFILE;

            Optional<DistanceTimeSpeed> motionAt = acceleration.getMotionAfterDuration(
                    carData, intercept.space, Duration.between(carData.getTime(), intercept.time), strikeProfile);

            if (motionAt.isPresent()) {
                DistanceTimeSpeed dts = motionAt.get();
                double interceptDistance = VectorUtil.flatDistance(myPosition, intercept.space);
                if (dts.getDistance() > interceptDistance) {
                    return Optional.of(intercept);
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
