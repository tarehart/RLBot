package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class DirectedSideHitStep implements Step {

    private static final double MANEUVER_SECONDS_PER_RADIAN = .1;
    private static final double APPROACH_DISTANCE = 1.5;
    private static final double SIDE_FLIP_SPEED = 9;
    private static final double DISTANCE_AT_CONTACT = 1.8;
    public static final double JUMP_TIME_PER_HEIGHT = .1;
    private Plan plan;

    private Vector3 originalIntercept;
    private LocalDateTime doneMoment;
    private KickStrategy kickStrategy;
    private Vector3 interceptModifier = null;
    private double maneuverSeconds = 0;

    private boolean finalApproach = false;

    public DirectedSideHitStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (doneMoment != null && input.time.isAfter(doneMoment)) {
            return Optional.empty();
        }

        final Optional<DirectedKickPlan> kickPlanOption;
        if (interceptModifier != null) {
            StrikeProfile strikeProfile = new StrikeProfile(maneuverSeconds, 0, 0);
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, true, interceptModifier, strikeProfile);
        } else {
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, true);
        }

        if (!kickPlanOption.isPresent()) {
            BallPath ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(4));
            return getNavigation(input, new SteerPlan(input.getMyCarData(), ballPath.getEndpoint().getSpace()));
        }

        DirectedKickPlan kickPlan = kickPlanOption.get();

        if (interceptModifier == null) {
            Vector3 nearSide = kickPlan.plannedKickForce.scaledToMagnitude(-(DISTANCE_AT_CONTACT + APPROACH_DISTANCE));
            interceptModifier = new Vector3(nearSide.x, nearSide.y, nearSide.z - 1.4); // Closer to ground
        }

        if (originalIntercept == null) {
            originalIntercept = kickPlan.ballAtIntercept.getSpace();
        } else {
            if (originalIntercept.distance(kickPlan.ballAtIntercept.getSpace()) > 30) {
                BotLog.println("Failed to make the directed kick", input.team);
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        Vector2 strikeDirection = kickPlan.plannedKickForce.flatten().normalized();
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();

        Vector2 orthogonalPoint = carPositionAtIntercept.flatten();

        if (finalApproach) {
            return performFinalApproach(input, orthogonalPoint, kickPlan, carPositionAtIntercept, strikeDirection);
        }

        double strikeTime = getStrikeTime(carPositionAtIntercept, APPROACH_DISTANCE);
        double expectedSpeed = kickPlan.distancePlot.getMotionAfterDistance(car.position.flatten().distance(orthogonalPoint)).map(m -> m.speed).orElse(40.0);
        double backoff = expectedSpeed * strikeTime + 1;

        Vector2 carToIntercept = carPositionAtIntercept.minus(car.position).flatten();
        Vector2 facingForSideFlip = VectorUtil.orthogonal(strikeDirection, v -> v.dotProduct(carToIntercept) > 0).normalized();

        Vector2 steerTarget = orthogonalPoint.minus(facingForSideFlip.scaled(backoff));

        Vector2 toOrthogonal = orthogonalPoint.minus(car.position.flatten());

        double distance = toOrthogonal.magnitude();
        Vector2 carNose = car.orientation.noseVector.flatten();
        double angle = Vector2.angle(carNose, facingForSideFlip);
        if (distance < backoff + 3 && angle < Math.PI / 8) {
            doneMoment = input.time.plus(TimeUtil.toDuration(strikeTime + .5));
            finalApproach = true;
            maneuverSeconds = 0;
            // Done with the circle turn. Drive toward the orthogonal point and wait for the right moment to launch.
            return performFinalApproach(input, orthogonalPoint, kickPlan, carPositionAtIntercept, strikeDirection);
        }


        maneuverSeconds = angle * MANEUVER_SECONDS_PER_RADIAN;

        SteerPlan circleTurnPlan = SteerUtil.getPlanForCircleTurn(car, kickPlan.distancePlot, steerTarget, facingForSideFlip);

        return getNavigation(input, circleTurnPlan);
    }

    private double getStrikeTime(Vector3 carPositionAtIntercept, double approachDistance) {
        double jumpTime = getJumpTime(carPositionAtIntercept);
        return jumpTime + approachDistance / SIDE_FLIP_SPEED;
    }

    private Optional<AgentOutput> performFinalApproach(AgentInput input, Vector2 orthogonalPoint, DirectedKickPlan kickPlan, Vector3 carPositionAtIntercept, Vector2 strikeDirection) {

        // You're probably darn close to flip time.

        CarData car = input.getMyCarData();

        double jumpTime = getJumpTime(carPositionAtIntercept);
        Vector2 carAtImpact = kickPlan.ballAtIntercept.space.flatten().plus(strikeDirection.scaled(-DISTANCE_AT_CONTACT));
        Vector2 toImpact = carAtImpact.minus(car.position.flatten());
        Vector2 projectedApproach = VectorUtil.project(toImpact, car.orientation.rightVector.flatten());
        double realApproachDistance = projectedApproach.magnitude();
        double strikeTime = getStrikeTime(carPositionAtIntercept, realApproachDistance);
        double backoff = car.velocity.magnitude() * strikeTime;

        double distance = car.position.flatten().distance(orthogonalPoint);
        if (distance < backoff) {
            // Time to launch!
            double strikeForceCorrection = DirectedKickUtil.getAngleOfKickFromApproach(car, kickPlan);
            plan = SetPieces.jumpSideFlip(strikeForceCorrection > 0, jumpTime);
            plan.begin();
            return plan.getOutput(input);
        } else {
            BotLog.println(String.format("Side flip soon. Distance: %.2f", distance), input.team);
            return Optional.of(SteerUtil.steerTowardGroundPosition(car, orthogonalPoint));
        }
    }

    private double getJumpTime(Vector3 carPositionAtIntercept) {
        return (carPositionAtIntercept.z - AirTouchPlanner.CAR_BASE_HEIGHT - .1) * JUMP_TIME_PER_HEIGHT;
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        if (car.boost == 0) {
            Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint);
            if (sensibleFlip.isPresent()) {
                BotLog.println("Front flip toward side hit", input.team);
                this.plan = sensibleFlip.get();
                this.plan.begin();
                return this.plan.getOutput(input);
            }
        }

        return Optional.of(circleTurnOption.immediateSteer);
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {

    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Directed Side Hit", plan);
    }
}
