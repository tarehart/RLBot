package tarehart.rlbot.carpredict;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.intercept.StrikeProfile;
import tarehart.rlbot.time.Duration;

import java.util.Optional;

public class CarInterceptPlanner {

    private static final int CAR_CONTACT_DISTANCE = 5;

    public static Optional<SpaceTime> getCarIntercept(
            CarData carData,
            CarPath enemyPath,
            DistancePlot acceleration) {

        Vector3 myPosition = carData.getPosition();

        for (int i = 0; i < enemyPath.getSlices().size(); i++) {
            CarSlice slice = enemyPath.getSlices().get(i);
            SpaceTime spaceTime = new SpaceTime(slice.space, slice.getTime());
            StrikeProfile strikeProfile = new StrikeProfile();
            Optional<DistanceTimeSpeed> motionAt = acceleration.getMotionAfterDuration(carData, spaceTime.space, Duration.between(carData.getTime(), spaceTime.time), strikeProfile);
            if (motionAt.isPresent()) {
                DistanceTimeSpeed dts = motionAt.get();
                double interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space);
                if (dts.getDistance() + CAR_CONTACT_DISTANCE > interceptDistance) {
                    if (i > 0) {
                        // Take the average of the current slice and the previous slice to avoid systematic pessimism.
                        CarSlice previousSlice = enemyPath.getSlices().get(i - 1);
                        double timeDiff = Duration.between(previousSlice.time, slice.time).getSeconds();
                        Vector3 toCurrentSlice = slice.space.minus(previousSlice.space);
                        return Optional.of(new SpaceTime(previousSlice.space.plus(toCurrentSlice.scaled(.5)), previousSlice.time.plusSeconds(timeDiff * .5)));

                    }
                    return Optional.of(spaceTime);
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }


}
