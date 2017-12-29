package tarehart.rlbot.planning;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.StrikeProfile;
import tarehart.rlbot.math.*;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.routing.CircleTurnUtil;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.util.Optional;

public class SteerUtil {

    public static final int SUPERSONIC_SPEED = 46;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 12;
    public static final double TURN_RADIUS_A = .0153;
    public static final double TURN_RADIUS_B = .16;
    public static final int TURN_RADIUS_C = 7;
    private static final double DEAD_ZONE = 0;

    public static Optional<SpaceTime> getCatchOpportunity(CarData carData, BallPath ballPath, double boostBudget) {

        GameTime searchStart = carData.getTime();

        double groundBounceEnergy = BallPhysics.getGroundBounceEnergy(ballPath.getStartPoint().getSpace().getZ(), ballPath.getStartPoint().getVelocity().getZ());

        if (groundBounceEnergy < 50) {
            return Optional.empty();
        }

        for (int i = 0; i < 3; i++) {
            Optional<BallSlice> landingOption = ballPath.getLanding(searchStart);

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

        GameTime searchStart = carData.getTime();

        Optional<BallSlice> landingOption = ballPath.getPlaneBreak(searchStart, new Plane(new Vector3(0, 0, height), new Vector3(0, 0, 1)), true);

        if (landingOption.isPresent()) {
            SpaceTime landing = landingOption.get().toSpaceTime();
            if (canGetUnder(carData, landing, boostBudget)) {
                return Optional.of(landing);
            }
        }

        return Optional.empty();
    }

    private static boolean canGetUnder(CarData carData, SpaceTime spaceTime, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), boostBudget, carData.getPosition().distance(spaceTime.space));

        Optional<DistanceTimeSpeed> dts = plot.getMotionAfterDuration(
                carData,
                spaceTime.space,
                Duration.between(carData.getTime(), spaceTime.time),
                new StrikeProfile());

        double requiredDistance = SteerUtil.getDistanceFromCar(carData, spaceTime.space);
        return dts.filter(travel -> travel.getDistance() > requiredDistance).isPresent();
    }

    public static double getCorrectionAngleRad(CarData carData, Vector3 target) {
        return getCorrectionAngleRad(carData, target.flatten());
    }

    public static double getCorrectionAngleRad(CarData carData, Vector2 target) {
        Vector2 noseVector = carData.getOrientation().getNoseVector().flatten();
        Vector2 toTarget = target.minus(carData.getPosition().flatten());
        return noseVector.correctionAngle(toTarget);
    }

    public static AgentOutput steerTowardGroundPosition(CarData carData, Vector2 position) {

        if (ArenaModel.isCarOnWall(carData)) {
            return steerTowardGroundPositionFromWall(carData, position);
        }

        WaypointTelemetry.set(position, carData.getTeam());

        double correctionAngle = getCorrectionAngleRad(carData, position);
        Vector2 myPositionFlat = carData.getPosition().flatten();
        double distance = position.distance(myPositionFlat);
        double speed = carData.getVelocity().magnitude();
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic(), false);
    }

    public static AgentOutput steerTowardGroundPosition(CarData carData, Vector2 position, boolean noBoosting) {

        if (ArenaModel.isCarOnWall(carData)) {
            return steerTowardGroundPositionFromWall(carData, position);
        }

        WaypointTelemetry.set(position, carData.getTeam());

        double correctionAngle = getCorrectionAngleRad(carData, position);
        Vector2 myPositionFlat = carData.getPosition().flatten();
        double distance = position.distance(myPositionFlat);
        double speed = carData.getVelocity().magnitude();
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic(), noBoosting);
    }

    private static AgentOutput steerTowardGroundPositionFromWall(CarData carData, Vector2 position) {
        Vector2 toPositionFlat = position.minus(carData.getPosition().flatten());
        Vector3 carShadow = new Vector3(carData.getPosition().getX(), carData.getPosition().getY(), 0);
        double heightOnWall = carData.getPosition().getZ();
        Vector3 wallNormal = carData.getOrientation().getRoofVector();
        double distanceOntoField = VectorUtil.project(toPositionFlat, wallNormal.flatten()).magnitude();
        double wallWeight = heightOnWall / (heightOnWall + distanceOntoField);
        Vector3 toPositionAlongSeam = new Vector3(toPositionFlat.getX(), toPositionFlat.getY(), 0).projectToPlane(wallNormal);
        Vector3 seamPosition = carShadow.plus(toPositionAlongSeam.scaled(wallWeight));

        return steerTowardWallPosition(carData, seamPosition);
    }

    public static AgentOutput steerTowardWallPosition(CarData carData, Vector3 position) {
        Vector3 toPosition = position.minus(carData.getPosition());
        double correctionAngle = VectorUtil.getCorrectionAngle(carData.getOrientation().getNoseVector(), toPosition, carData.getOrientation().getRoofVector());
        double speed = carData.getVelocity().magnitude();
        double distance = position.distance(carData.getPosition());
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic(), false);
    }

    private static AgentOutput getSteeringOutput(double correctionAngle, double distance, double speed, boolean isSupersonic, boolean noBoosting) {
        double difference = Math.abs(correctionAngle);
        double turnSharpness = difference * 6/Math.PI + difference * speed * .1;
        //turnSharpness = (1 - DEAD_ZONE) * turnSharpness + Math.signum(turnSharpness) * DEAD_ZONE;

        boolean shouldBrake = distance < 25 && difference > Math.PI / 4 && speed > 25;
        boolean shouldSlide = shouldBrake || difference > Math.PI / 2;
        boolean shouldBoost = !noBoosting && !shouldBrake && difference < Math.PI / 6 && !isSupersonic;

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
        return VectorUtil.flatDistance(loc, car.getPosition());
    }

    public static Optional<Plan> getSensibleFlip(CarData car, Vector3 target) {
        return getSensibleFlip(car, target.flatten());
    }

    public static Optional<Plan> getSensibleFlip(CarData car, Vector2 target) {

        Vector2 toTarget = target.minus(car.getPosition().flatten());
        if (toTarget.magnitude() > 40 &&
                Vector2.Companion.angle(car.getOrientation().getNoseVector().flatten(), toTarget) > 3 * Math.PI / 4 &&
                (car.getVelocity().flatten().dotProduct(toTarget) > 0 || car.getVelocity().magnitude() < 5)) {

            return Optional.of(SetPieces.halfFlip(target));
        }

        double speed = car.getVelocity().flatten().magnitude();
        if(car.isSupersonic() || car.getBoost() > 75 || speed < AccelerationModel.FLIP_THRESHOLD_SPEED) {
            return Optional.empty();
        }

        double distanceCovered = AccelerationModel.getFrontFlipDistance(speed);


        double distanceToIntercept = toTarget.magnitude();
        if (distanceToIntercept > distanceCovered + 10) {

            Vector2 facing = car.getOrientation().getNoseVector().flatten();
            double facingCorrection = facing.correctionAngle(toTarget);
            double slideAngle = facing.correctionAngle(car.getVelocity().flatten());

            if (Math.abs(facingCorrection) < GOOD_ENOUGH_ANGLE && Math.abs(slideAngle) < GOOD_ENOUGH_ANGLE) {
                return Optional.of(SetPieces.frontFlip());
            }
        }

        return Optional.empty();
    }

    public static AgentOutput getThereOnTime(CarData input, SpaceTime groundPositionAndTime) {
        double flatDistance = VectorUtil.flatDistance(input.getPosition(), groundPositionAndTime.space);

        double secondsTillAppointment = Duration.between(input.getTime(), groundPositionAndTime.time).toMillis() / 1000.0;
        double speed = input.getVelocity().magnitude();

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

}
