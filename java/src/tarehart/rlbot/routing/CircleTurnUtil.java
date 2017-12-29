package tarehart.rlbot.routing;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Circle;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.SteerUtil;

import java.util.Optional;

public class CircleTurnUtil {
    public static SteerPlan planWithinCircle(CarData car, Vector2 targetPosition, Vector2 targetFacing, double currentSpeed) {

        Vector2 targetNose = targetPosition.plus(targetFacing);
        Vector2 targetTail = targetPosition.minus(targetFacing);
        Vector2 facing = car.orientation.noseVector.flatten();

        Vector2 flatPosition = car.position.flatten();
        Circle idealCircle = Circle.getCircleFromPoints(targetTail, targetNose, flatPosition);

        boolean clockwise = Circle.isClockwise(idealCircle, targetPosition, targetFacing);

        Vector2 centerToMe = flatPosition.minus(idealCircle.center);
        Vector2 idealDirection = VectorUtil.orthogonal(centerToMe, v -> Circle.isClockwise(idealCircle, flatPosition, v) == clockwise).normalized();

//        if (facing.dotProduct(idealDirection) < .7) {
//            AgentOutput output = steerTowardGroundPosition(car, flatPosition.plus(idealDirection));
//            return new SteerPlan(output, targetPosition);
//        }

        Optional<Double> idealSpeedOption = getSpeedForRadius(idealCircle.radius);

        double idealSpeed = idealSpeedOption.orElse(5.0);

        double speedRatio = currentSpeed / idealSpeed; // Ideally should be 1

        double lookaheadRadians = Math.PI / 20;
        Vector2 centerToSteerTarget = VectorUtil.rotateVector(flatPosition.minus(idealCircle.center), lookaheadRadians * (clockwise ? -1 : 1));
        Vector2 steerTarget = idealCircle.center.plus(centerToSteerTarget);

        AgentOutput output = SteerUtil.steerTowardGroundPosition(car, steerTarget).withBoost(false).withSlide(false).withDeceleration(0).withAcceleration(1);

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

        return new SteerPlan(output, flatPosition, targetPosition, targetFacing, idealCircle);
    }

    private static double getTurnRadius(double speed) {
        return SteerUtil.TURN_RADIUS_A * speed * speed + SteerUtil.TURN_RADIUS_B * speed + SteerUtil.TURN_RADIUS_C;
    }

    private static Optional<Double> getSpeedForRadius(double radius) {

        if (radius == SteerUtil.TURN_RADIUS_C) {
            return Optional.of(0d);
        }

        if (radius < SteerUtil.TURN_RADIUS_C) {
            return Optional.empty();
        }

        double a = SteerUtil.TURN_RADIUS_A;
        double b = SteerUtil.TURN_RADIUS_B;
        double c = SteerUtil.TURN_RADIUS_C - radius;

        double p = -b / (2 * a);
        double q = Math.sqrt(b * b - 4 * a * c) / (2 * a);
        return Optional.of(p + q);
    }

    public static double getFacingCorrectionSeconds(Vector2 approach, Vector2 targetFacing, double expectedSpeed) {

        double correction = approach.correctionAngle(targetFacing);
        return getTurnRadius(expectedSpeed) * Math.abs(correction) / expectedSpeed;
    }

    public static SteerPlan getPlanForCircleTurn(
            CarData car, DistancePlot distancePlot, Vector2 targetPosition, Vector2 targetFacing) {

        double distance = car.position.flatten().distance(targetPosition);
        double maxSpeed = distancePlot.getMotionAfterDistance(distance)
                .map(dts -> dts.speed)
                .orElse(AccelerationModel.SUPERSONIC_SPEED);
        double idealSpeed = getIdealCircleSpeed(car, targetFacing);
        double currentSpeed = car.velocity.magnitude();

        return circleWaypoint(car, targetPosition, targetFacing, currentSpeed, Math.min(maxSpeed, idealSpeed));
    }

    private static double getIdealCircleSpeed(CarData car, Vector2 targetFacing) {
        double orientationCorrection = car.orientation.noseVector.flatten().correctionAngle(targetFacing);
        double angleAllowingFullSpeed = Math.PI / 6;
        double speedPenaltyPerRadian = 20;
        double rawPenalty = speedPenaltyPerRadian * (Math.abs(orientationCorrection) - angleAllowingFullSpeed);
        double correctionPenalty = Math.max(0, rawPenalty);
        return Math.max(15, AccelerationModel.SUPERSONIC_SPEED - correctionPenalty);
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

        return new SteerPlan(SteerUtil.steerTowardGroundPosition(car, tangentPoint), tangentPoint, targetPosition, targetFacing, new Circle(center, turnRadius));
    }
}
