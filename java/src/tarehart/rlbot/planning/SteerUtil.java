package tarehart.rlbot.planning;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.physics.DistancePlot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.BiPredicate;

public class SteerUtil {

    public static final int SUPERSONIC_SPEED = 46;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 12;
    public static final double TURN_RADIUS_A = .0153;
    public static final double TURN_RADIUS_B = .16;
    public static final int TURN_RADIUS_C = 7;
    private static final double DEAD_ZONE = .2;

    public static Optional<SpaceTime> getCatchOpportunity(CarData carData, BallPath ballPath, double boostBudget) {

        LocalDateTime searchStart = carData.time;

        double groundBounceEnergy = BallPhysics.getGroundBounceEnergy(ballPath.getStartPoint());

        if (groundBounceEnergy < 50) {
            return Optional.empty();
        }

        for (int i = 0; i < 3; i++) {
            Optional<SpaceTimeVelocity> landingOption = ballPath.getLanding(searchStart);

            if (landingOption.isPresent()) {
                SpaceTime landing = landingOption.get().toSpaceTime();
                if (canGetUnder(carData, landing, boostBudget)) {
                    return Optional.of(landing);
                } else {
                    searchStart = landing.time.plusSeconds(1);
                }
            } else {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public static Optional<SpaceTime> getVolleyOpportunity(CarData carData, BallPath ballPath, double boostBudget, double height) {

        LocalDateTime searchStart = carData.time;

        Optional<SpaceTimeVelocity> landingOption = ballPath.getPlaneBreak(searchStart, new Plane(new Vector3(0, 0, height), new Vector3(0, 0, 1)), true);

        if (landingOption.isPresent()) {
            SpaceTime landing = landingOption.get().toSpaceTime();
            if (canGetUnder(carData, landing, boostBudget)) {
                return Optional.of(landing);
            }
        }

        return Optional.empty();
    }

    private static boolean canGetUnder(CarData carData, SpaceTime spaceTime, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), boostBudget, carData.position.distance(spaceTime.space));
        Optional<DistanceTimeSpeed> dts = plot.getMotionAfterStrike(carData, spaceTime, null);
        double requiredDistance = SteerUtil.getDistanceFromCar(carData, spaceTime.space);
        return dts.filter(travel -> travel.distance > requiredDistance).isPresent();
    }

    public static Optional<SpaceTime> getInterceptOpportunityAssumingMaxAccel(CarData carData, BallPath ballPath, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), boostBudget);

        return getInterceptOpportunity(carData, ballPath, plot);
    }

    public static Optional<SpaceTime> getInterceptOpportunity(CarData carData, BallPath ballPath, DistancePlot acceleration) {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, new Vector3(), (a, b) -> true);
    }

    public static Optional<SpaceTime> getFilteredInterceptOpportunity(
            CarData carData, BallPath ballPath, DistancePlot acceleration, Vector3 interceptModifier, BiPredicate<CarData, SpaceTime> predicate) {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, interceptModifier, predicate, null);
    }

    public static Optional<SpaceTime> getFilteredInterceptOpportunity(
            CarData carData,
            BallPath ballPath,
            DistancePlot acceleration,
            Vector3 interceptModifier,
            BiPredicate<CarData, SpaceTime> predicate,
            StrikeProfile strikeProfile) {

        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, interceptModifier, predicate, strikeProfile, new Vector3(0, 0, 1));
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param acceleration
     * @param interceptModifier an offset from the ball position that the car is trying to reach
     * @param predicate determines whether a particular ball slice is eligible for intercept
     * @param strikeProfile a description of how the car will move during the final moments of the intercept
     * @param planeNormal the normal of the plane that the car is driving on for this intercept.
     * @return
     */
    public static Optional<SpaceTime> getFilteredInterceptOpportunity(
            CarData carData,
            BallPath ballPath,
            DistancePlot acceleration,
            Vector3 interceptModifier,
            BiPredicate<CarData, SpaceTime> predicate,
            StrikeProfile strikeProfile,
            Vector3 planeNormal) {

        Vector3 myPosition = carData.position;

        for (SpaceTimeVelocity ballMoment: ballPath.getSlices()) {
            SpaceTime intercept = new SpaceTime(ballMoment.space.plus(interceptModifier), ballMoment.getTime());
            Optional<DistanceTimeSpeed> motionAt = acceleration.getMotionAfterStrike(carData, intercept, strikeProfile);
            if (motionAt.isPresent()) {
                DistanceTimeSpeed dts = motionAt.get();
                double interceptDistance = VectorUtil.flatDistance(myPosition, intercept.space, planeNormal);
                if (dts.distance > interceptDistance) {
                    if (predicate.test(carData, intercept)) {
                        return Optional.of(intercept);
                    }
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static double getCorrectionAngleRad(CarData carData, Vector3 target) {
        return getCorrectionAngleRad(carData, target.flatten());
    }

    public static double getCorrectionAngleRad(CarData carData, Vector2 target) {
        Vector2 noseVector = carData.orientation.noseVector.flatten();
        Vector2 toTarget = target.minus(carData.position.flatten());
        return noseVector.correctionAngle(toTarget);
    }

    public static AgentOutput steerTowardGroundPosition(CarData carData, Vector2 position) {

        if (ArenaModel.isCarOnWall(carData)) {
            return steerTowardGroundPositionFromWall(carData, position);
        }

        double correctionAngle = getCorrectionAngleRad(carData, position);
        Vector2 myPositionFlat = carData.position.flatten();
        double distance = position.distance(myPositionFlat);
        double speed = carData.velocity.magnitude();
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic);
    }

    private static AgentOutput steerTowardGroundPositionFromWall(CarData carData, Vector2 position) {
        Vector2 toPositionFlat = position.minus(carData.position.flatten());
        Vector3 carShadow = new Vector3(carData.position.x, carData.position.y, 0);
        double heightOnWall = carData.position.z;
        Vector3 wallNormal = carData.orientation.roofVector;
        double distanceOntoField = VectorUtil.project(toPositionFlat, wallNormal.flatten()).magnitude();
        double wallWeight = heightOnWall / (heightOnWall + distanceOntoField);
        Vector3 toPositionAlongSeam = new Vector3(toPositionFlat.x, toPositionFlat.y, 0).projectToPlane(wallNormal);
        Vector3 seamPosition = carShadow.plus(toPositionAlongSeam.scaled(wallWeight));

        return steerTowardWallPosition(carData, seamPosition);
    }

    public static AgentOutput steerTowardWallPosition(CarData carData, Vector3 position) {
        Vector3 toPosition = position.minus(carData.position);
        double correctionAngle = VectorUtil.getCorrectionAngle(carData.orientation.noseVector, toPosition, carData.orientation.roofVector);
        double speed = carData.velocity.magnitude();
        double distance = position.distance(carData.position);
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic);
    }

    private static AgentOutput getSteeringOutput(double correctionAngle, double distance, double speed, boolean isSupersonic) {
        double difference = Math.abs(correctionAngle);
        double turnSharpness = difference * 6/Math.PI + difference * speed * .1;
        turnSharpness = (1 - DEAD_ZONE) * turnSharpness + Math.signum(turnSharpness) * DEAD_ZONE;

        boolean shouldBrake = distance < 25 && difference > Math.PI / 4 && speed > 25;
        boolean shouldSlide = shouldBrake || difference > Math.PI / 2;
        boolean shouldBoost = !shouldBrake && difference < Math.PI / 6 && !isSupersonic;

        return new AgentOutput()
                .withAcceleration(shouldBrake ? 0 : 1)
                .withDeceleration(shouldBrake ? 1 : 0)
                .withSteer((float) (-Math.signum(correctionAngle) * turnSharpness))
                .withSlide(shouldSlide)
                .withBoost(shouldBoost);
    }

    public static AgentOutput steerTowardGroundPosition(CarData carData, Vector3 position) {
        return steerTowardGroundPosition(carData, position.flatten());
    }

    public static double getDistanceFromCar(CarData car, Vector3 loc) {
        return VectorUtil.flatDistance(loc, car.position);
    }

    public static Optional<Plan> getSensibleFlip(CarData car, Vector3 target) {
        return getSensibleFlip(car, target.flatten());
    }

    public static Optional<Plan> getSensibleFlip(CarData car, Vector2 target) {

        if(car.isSupersonic) {
            return Optional.empty();
        }

        double distanceCovered = AccelerationModel.getFrontFlipDistance(car.velocity.magnitude());

        Vector2 toTarget = target.minus(car.position.flatten());
        double distanceToIntercept = toTarget.magnitude();
        if (distanceToIntercept > distanceCovered + 10) {

            Vector2 facing = car.orientation.noseVector.flatten();
            double facingCorrection = facing.correctionAngle(toTarget);
            double slideAngle = facing.correctionAngle(car.velocity.flatten());

            if (Math.abs(facingCorrection) < GOOD_ENOUGH_ANGLE && Math.abs(slideAngle) < GOOD_ENOUGH_ANGLE
                    && car.velocity.magnitude() > SteerUtil.SUPERSONIC_SPEED / 4) {

                return Optional.of(SetPieces.frontFlip());
            }
        }

        return Optional.empty();
    }

    public static AgentOutput getThereOnTime(CarData input, SpaceTime groundPositionAndTime) {
        double flatDistance = VectorUtil.flatDistance(input.position, groundPositionAndTime.space);

        double secondsTillAppointment = Duration.between(input.time, groundPositionAndTime.time).toMillis() / 1000.0;
        double speed = input.velocity.magnitude();

        double pace = speed * secondsTillAppointment / flatDistance; // Ideally this should be 1

        if (flatDistance > 40) {
            // Go fast
            return SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space);
        } else if (flatDistance > 10 && pace < 1) {
            // Go fast
            return SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space);
        } else if (pace < 1) {
            // Go moderate
            return SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space).withBoost(false);
        } else {
            // We're going too fast!
            AgentOutput agentOutput = SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space);
            agentOutput.withAcceleration(0).withBoost(false).withDeceleration(Math.max(0, pace - 1.5)); // Hit the brakes, but keep steering!
            return agentOutput;
        }
    }

    private static double getTurnRadius(double speed) {
        return TURN_RADIUS_A * speed * speed + TURN_RADIUS_B * speed + TURN_RADIUS_C;
    }

    private static Optional<Double> getSpeedForRadius(double radius) {

        if (radius == TURN_RADIUS_C) {
            return Optional.of(0d);
        }

        if (radius < TURN_RADIUS_C) {
            return Optional.empty();
        }

        double a = TURN_RADIUS_A;
        double b = TURN_RADIUS_B;
        double c = TURN_RADIUS_C - radius;

        double p = -b / (2 * a);
        double q = Math.sqrt(b * b - 4 * a * c) / (2 * a);
        return Optional.of(p + q);
    }

    public static double getFacingCorrectionSeconds(Vector2 approach, Vector2 targetFacing, double expectedSpeed) {

        double correction = approach.correctionAngle(targetFacing);
        return getTurnRadius(expectedSpeed) * Math.abs(correction) / expectedSpeed;
    }

    public static Optional<Vector2> getWaypointForCircleTurn(
            CarData car, DistancePlot distancePlot, Vector2 targetPosition, Vector2 targetFacing) {

        return Optional.of(getPlanForCircleTurn(car, distancePlot, targetPosition, targetFacing).waypoint);
    }

    public static SteerPlan getPlanForCircleTurn(
            CarData car, DistancePlot distancePlot, Vector2 targetPosition, Vector2 targetFacing) {

        Vector2 flatPosition = car.position.flatten();
        double distance = flatPosition.distance(targetPosition);
        double currentSpeed = car.velocity.magnitude();
        double expectedSpeed = AccelerationModel.SUPERSONIC_SPEED;

        Optional<DistanceTimeSpeed> motion = distancePlot.getMotionAfterDistance(distance);
        if (motion.isPresent()) {
            expectedSpeed = motion.get().speed;
        }
        return circleWaypoint(car, targetPosition, targetFacing, currentSpeed, expectedSpeed);
    }

    private static SteerPlan circleWaypoint(CarData car, Vector2 targetPosition, Vector2 targetFacing, double currentSpeed, double expectedSpeed) {

        Vector2 flatPosition = car.position.flatten();
        Vector2 toTarget = targetPosition.minus(flatPosition);

        double turnRadius = getTurnRadius(expectedSpeed);
        // Make sure the radius vector points from the target position to the center of the turn circle.
        Vector2 radiusVector = VectorUtil.orthogonal(targetFacing, v -> v.dotProduct(toTarget) < 0).scaled(turnRadius);

        Vector2 center = targetPosition.plus(radiusVector);
        double distanceFromCenter = flatPosition.distance(center);

        Vector2 centerToTangent = VectorUtil.orthogonal(toTarget, v -> v.dotProduct(targetFacing) < 0).scaledToMagnitude(turnRadius);
        Vector2 tangentPoint = center.plus(centerToTangent);

        if (distanceFromCenter < turnRadius * 1.1) {

            if (currentSpeed < expectedSpeed) {
                return circleWaypoint(car, targetPosition, targetFacing, currentSpeed, currentSpeed);
            }

            return planWithinCircle(car, targetPosition, targetFacing, currentSpeed);
        }

        return new SteerPlan(steerTowardGroundPosition(car, tangentPoint), tangentPoint);
    }

    private static SteerPlan planWithinCircle(CarData car, Vector2 targetPosition, Vector2 targetFacing, double currentSpeed) {

        Vector2 targetNose = targetPosition.plus(targetFacing);
        Vector2 targetTail = targetPosition.minus(targetFacing);
        Vector2 facing = car.orientation.noseVector.flatten();

        Vector2 flatPosition = car.position.flatten();
        Circle idealCircle = Circle.getCircleFromPoints(targetTail, targetNose, flatPosition);

        boolean clockwise = Circle.isClockwise(idealCircle, targetPosition, targetFacing);

        Vector2 centerToMe = car.position.flatten().minus(idealCircle.center);
        Vector2 idealDirection = VectorUtil.orthogonal(centerToMe, v -> Circle.isClockwise(idealCircle, flatPosition, v) == clockwise).normalized();

        if (facing.dotProduct(idealDirection) < .7) {
            AgentOutput output = steerTowardGroundPosition(car, flatPosition.plus(idealDirection));
            return new SteerPlan(output, targetPosition);
        }

        Optional<Double> idealSpeedOption = getSpeedForRadius(idealCircle.radius);

        double idealSpeed = idealSpeedOption.orElse(5.0);

        double speedRatio = currentSpeed / idealSpeed; // Ideally should be 1

        double lookaheadRadians = Math.PI / 20;
        Vector2 centerToSteerTarget = VectorUtil.rotateVector(flatPosition.minus(idealCircle.center), lookaheadRadians * (clockwise ? -1 : 1));
        Vector2 steerTarget = idealCircle.center.plus(centerToSteerTarget);

        AgentOutput output = steerTowardGroundPosition(car, steerTarget).withBoost(false).withSlide(false).withDeceleration(0).withAcceleration(1);

        if (speedRatio < 1) {
            output.withBoost(currentSpeed >= AccelerationModel.MEDIUM_SPEED && speedRatio < .8 || speedRatio < .7);
        } else {
            int framesBetweenSlidePulses;
            if (speedRatio > 2) {
                framesBetweenSlidePulses = 3;
            } else if (speedRatio > 1.5) {
                framesBetweenSlidePulses = 6;
            } else if (speedRatio > 1.2) {
                framesBetweenSlidePulses = 9;
            } else {
                framesBetweenSlidePulses = 12;
            }
            output.withSlide(car.frameCount % (framesBetweenSlidePulses + 1) == 0);
        }

        return new SteerPlan(output, targetPosition);
    }
}
