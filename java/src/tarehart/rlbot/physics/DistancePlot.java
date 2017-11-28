package tarehart.rlbot.physics;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.StrikeProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DistancePlot {

    ArrayList<DistanceTimeSpeed> plot = new ArrayList<>();

    public DistancePlot(DistanceTimeSpeed start) {
        plot.add(start);
    }

    public void addSlice(DistanceTimeSpeed dts) {
        plot.add(dts);
    }

    public List<DistanceTimeSpeed> getSlices() {
        return plot;
    }

    public Optional<DistanceTimeSpeed> getMotionAfterSeconds(double time) {
        if (time < plot.get(0).getTime() || time > plot.get(plot.size() - 1).getTime()) {
            return Optional.empty();
        }

        for (int i = 0; i < plot.size() - 1; i++) {
            DistanceTimeSpeed current = plot.get(i);
            DistanceTimeSpeed next = plot.get(i + 1);
            if (next.getTime() > time) {

                double simulationStepSeconds = next.getTime() - current.getTime();
                double tweenPoint = (time - current.getTime()) / simulationStepSeconds;
                double distance = (1 - tweenPoint) * current.distance + tweenPoint * next.distance;
                double speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed;
                return Optional.of(new DistanceTimeSpeed(distance, time, speed));
            }
        }

        return Optional.of(plot.get(plot.size() - 1));
    }

    public Optional<DistanceTimeSpeed> getMotionAfterDistance(double distance) {

        for (int i = 0; i < plot.size() - 1; i++) {
            DistanceTimeSpeed current = plot.get(i);
            DistanceTimeSpeed next = plot.get(i + 1);
            if (next.distance > distance) {
                double simulationStepSeconds = next.getTime() - current.getTime();
                double tweenPoint = (distance - current.distance) / (next.distance - current.distance);
                double moment = current.getTime() + simulationStepSeconds * tweenPoint;
                double speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed;
                return Optional.of(new DistanceTimeSpeed(distance, moment, speed));
            }
        }
        return Optional.empty();
    }

    public Optional<Double> getTravelTime(double distance) {
        Optional<DistanceTimeSpeed> motionAt = getMotionAfterDistance(distance);
        return motionAt.map(DistanceTimeSpeed::getTime);
    }

    /**
     * The spacetime is used to infer allotted time and correction angle, NOT distance.
     */
    public Optional<DistanceTimeSpeed> getMotionAfterStrike(CarData carData, SpaceTime spaceTime, StrikeProfile strikeProfile) {

        double orientSeconds = AccelerationModel.getSteerPenaltySeconds(carData, spaceTime.space);

        double totalSeconds = TimeUtil.secondsBetween(carData.time, spaceTime.time);
        double secondsSpentAccelerating = Math.max(0, totalSeconds - orientSeconds);

        if (strikeProfile == null || strikeProfile.dodgeSeconds == 0 || strikeProfile.speedBoost == 0) {
            Optional<DistanceTimeSpeed> motion = getMotionAfterSeconds(secondsSpentAccelerating);
            return motion.map(dts -> new DistanceTimeSpeed(dts.distance, totalSeconds, dts.speed));
        }

        double speedupSeconds = strikeProfile.dodgeSeconds;
        double speedBoost = strikeProfile.speedBoost;
        if (secondsSpentAccelerating < speedupSeconds) {
            // Not enough time for a full strike.
            double beginningSpeed = plot.get(0).speed;
            double increasedSpeed = Math.min(beginningSpeed + speedBoost, AccelerationModel.SUPERSONIC_SPEED);
            return Optional.of(new DistanceTimeSpeed(increasedSpeed * secondsSpentAccelerating, totalSeconds, increasedSpeed));
        }

        double accelSecondsBeforeStrike = secondsSpentAccelerating - speedupSeconds;
        Optional<DistanceTimeSpeed> dtsOption = getMotionAfterSeconds(accelSecondsBeforeStrike);

        if (dtsOption.isPresent()) {
            DistanceTimeSpeed dts = dtsOption.get();
            double beginningSpeed = dts.speed;
            double increasedSpeed = Math.min(beginningSpeed + speedBoost, AccelerationModel.SUPERSONIC_SPEED);
            return Optional.of(new DistanceTimeSpeed(dts.distance + increasedSpeed * speedupSeconds, totalSeconds, increasedSpeed));
        } else {
            // We ran out of data in the distance plot.
            return Optional.empty();
        }
    }
}
