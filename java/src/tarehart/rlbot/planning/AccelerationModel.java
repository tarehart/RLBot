package tarehart.rlbot.planning;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.time.Duration;

import java.util.Optional;

public class AccelerationModel {

    public static final double SUPERSONIC_SPEED = 46;
    public static final double MEDIUM_SPEED = 28;
    public static final double FRONT_FLIP_SECONDS = 1.5;

    private static final Double TIME_STEP = 0.1;
    private static final double FRONT_FLIP_SPEED_BOOST = 10;
    private static final double SUB_MEDIUM_ACCELERATION = 15; // zero to medium in about 2 seconds.
    private static final double INCREMENTAL_BOOST_ACCELERATION = 8;
    private static final double AIR_BOOST_ACCELERATION = 12;
    private static final double BOOST_CONSUMED_PER_SECOND = 25;
    public static final double FLIP_THRESHOLD_SPEED = 20;


    public static Optional<Double> getTravelSeconds(CarData carData, DistancePlot plot, Vector3 target) {
        double distance = carData.position.distance(target);
        Optional<Double> travelTime = plot.getTravelTime(distance);
        double penaltySeconds = getSteerPenaltySeconds(carData, target);
        return travelTime.map(time -> time + penaltySeconds);
    }

    public static double getSteerPenaltySeconds(CarData carData, Vector3 target) {
        Vector3 toTarget = target.minus(carData.position);
        double correctionAngleRad = VectorUtil.getCorrectionAngle(carData.orientation.noseVector, toTarget, carData.orientation.roofVector);
        double correctionErr = Math.abs(correctionAngleRad);

        if (correctionErr < Math.PI / 6) {
            return 0;
        }
        return correctionErr * .1 + correctionErr * carData.velocity.magnitude() * .005;
    }

    public static DistancePlot simulateAcceleration(CarData carData, Duration duration, double boostBudget) {
        return simulateAcceleration(carData, duration, boostBudget, Double.MAX_VALUE);
    }

    public static DistancePlot simulateAcceleration(CarData carData, Duration duration, double boostBudget, double flipCutoffDistance) {

        double currentSpeed = VectorUtil.project(carData.velocity, carData.orientation.noseVector).magnitude();
        DistancePlot plot = new DistancePlot(new DistanceTimeSpeed(0, 0, currentSpeed));

        double boostRemaining = boostBudget;

        double distanceSoFar = 0;
        double secondsSoFar = 0;

        double secondsToSimulate = duration.getSeconds();

        while (secondsSoFar < secondsToSimulate) {
            double hypotheticalFrontFlipDistance = getFrontFlipDistance(currentSpeed);

            secondsSoFar += TIME_STEP;
            distanceSoFar += currentSpeed * TIME_STEP;

            if (currentSpeed >= SUPERSONIC_SPEED) {
                // It gets boring from now on. Put a slice at the very end.
                double secondsRemaining = secondsToSimulate - secondsSoFar;
                plot.addSlice(new DistanceTimeSpeed(distanceSoFar + SUPERSONIC_SPEED * secondsRemaining, secondsToSimulate, SUPERSONIC_SPEED));
                break;
            } else if (boostRemaining <= 0 && currentSpeed > FLIP_THRESHOLD_SPEED && distanceSoFar + hypotheticalFrontFlipDistance < flipCutoffDistance) {
                currentSpeed += FRONT_FLIP_SPEED_BOOST;
                if (currentSpeed > SUPERSONIC_SPEED) {
                    currentSpeed = SUPERSONIC_SPEED;
                }
                plot.addSlice(new DistanceTimeSpeed(distanceSoFar, secondsSoFar, currentSpeed));
                secondsSoFar += FRONT_FLIP_SECONDS;
                distanceSoFar += hypotheticalFrontFlipDistance;
                plot.addSlice(new DistanceTimeSpeed(distanceSoFar, secondsSoFar, currentSpeed));

            } else {
                double acceleration = getAcceleration(currentSpeed, boostRemaining > 0);
                currentSpeed += acceleration * TIME_STEP;
                if (currentSpeed > SUPERSONIC_SPEED) {
                    currentSpeed = SUPERSONIC_SPEED;
                }
                boostRemaining -= BOOST_CONSUMED_PER_SECOND * TIME_STEP;
                plot.addSlice(new DistanceTimeSpeed(distanceSoFar, secondsSoFar, currentSpeed));
            }
        }

        return plot;
    }

    private static double getAcceleration(double currentSpeed, boolean hasBoost) {

        if (currentSpeed >= SUPERSONIC_SPEED || !hasBoost && currentSpeed >= MEDIUM_SPEED) {
            return 0;
        }

        double accel = 0;
        if (currentSpeed < MEDIUM_SPEED) {
            accel += SUB_MEDIUM_ACCELERATION;
        }
        if (hasBoost) {
            accel += INCREMENTAL_BOOST_ACCELERATION;
        }

        return accel;
    }

    public static double getFrontFlipDistance(double speed) {
        return (speed + FRONT_FLIP_SPEED_BOOST) * FRONT_FLIP_SECONDS;
    }

    public static DistancePlot simulateAirAcceleration(CarData car, Duration duration, Vector3 averageNoseVector) {
        double currentSpeed = car.velocity.flatten().magnitude();
        DistancePlot plot = new DistancePlot(new DistanceTimeSpeed(0, 0, currentSpeed));

        double boostRemaining = car.boost;

        double distanceSoFar = 0;
        double secondsSoFar = 0;

        double secondsToSimulate = duration.getSeconds();

        while (secondsSoFar < secondsToSimulate) {

            double acceleration = boostRemaining > 0 ? averageNoseVector.flatten().magnitude() * AIR_BOOST_ACCELERATION : 0;
            currentSpeed += acceleration * TIME_STEP;
            if (currentSpeed > SUPERSONIC_SPEED) {
                currentSpeed = SUPERSONIC_SPEED;
            }
            distanceSoFar += currentSpeed * TIME_STEP;
            secondsSoFar += TIME_STEP;
            boostRemaining -= BOOST_CONSUMED_PER_SECOND * TIME_STEP;
            plot.addSlice(new DistanceTimeSpeed(distanceSoFar, secondsSoFar, currentSpeed));

            if (currentSpeed >= SUPERSONIC_SPEED) {
                // It gets boring from now on. Put a slice at the very end.
                double secondsRemaining = secondsToSimulate - secondsSoFar;
                plot.addSlice(new DistanceTimeSpeed(distanceSoFar + SUPERSONIC_SPEED * secondsRemaining, secondsToSimulate, SUPERSONIC_SPEED));
                break;
            }
        }

        return plot;
    }
}
