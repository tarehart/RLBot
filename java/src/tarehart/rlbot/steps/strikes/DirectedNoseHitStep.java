package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class DirectedNoseHitStep implements Step {

    public static final double MAX_NOSE_HIT_ANGLE = Math.PI / 18;
    private static final double MANEUVER_SECONDS_PER_RADIAN = .1;
    private Plan plan;

    private Vector3 originalIntercept;
    private LocalDateTime doneMoment;
    private KickStrategy kickStrategy;
    private Vector3 interceptModifier = null;
    private double maneuverSeconds = 0;
    private Double circleBackoff = null;

    private double estimatedAngleOfKickFromApproach;

    public DirectedNoseHitStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public static boolean canMakeDirectedKick(AgentInput input, KickStrategy kickStrategy) {
        boolean tooBouncy = BallPhysics.getGroundBounceEnergy(
                new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity)) > 30;

        Vector2 kickDirection = kickStrategy.getKickDirection(input).flatten();
        Vector2 carToBall = input.ballPosition.minus(input.getMyCarData().position).flatten();

        double angle = Vector2.angle(kickDirection, carToBall);

        boolean wrongSide = angle > Math.PI * 2/3;

        return !tooBouncy && !wrongSide;
    }

    public double getEstimatedAngleOfKickFromApproach() {
        return estimatedAngleOfKickFromApproach;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (doneMoment == null && car.position.distance(input.ballPosition) < 4.5) {
            // You get a tiny bit more time
            doneMoment = input.time.plus(Duration.ofMillis(200));
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (doneMoment != null && input.time.isAfter(doneMoment)) {
            return Optional.empty();
        }

        if (ArenaModel.isCarOnWall(car)) {
            return Optional.empty();
        }

        final Optional<DirectedKickPlan> kickPlanOption;
        if (interceptModifier != null) {

            StrikeProfile strikeProfile = new StrikeProfile(maneuverSeconds, 0, 0);
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, false, interceptModifier, strikeProfile);
        } else {
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, false);
        }

        if (!kickPlanOption.isPresent()) {
            return Optional.of(SteerUtil.steerTowardGroundPosition(input.getMyCarData(), input.ballPosition));
        }

        DirectedKickPlan kickPlan = kickPlanOption.get();

        if (originalIntercept == null) {
            originalIntercept = kickPlan.ballAtIntercept.getSpace();
        } else {
            if (originalIntercept.distance(kickPlan.ballAtIntercept.getSpace()) > 30) {
                BotLog.println("Failed to make the nose hit", input.team);
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        if (circleBackoff == null) {
            circleBackoff = car.position.distance(kickPlan.ballAtIntercept.getSpace()) > 60 ? 5.0 : 1.0;
        }

        Vector2 strikeForceFlat = kickPlan.plannedKickForce.flatten().normalized();
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();
        Vector2 carToIntercept = carPositionAtIntercept.minus(car.position).flatten();
        estimatedAngleOfKickFromApproach = DirectedKickUtil.getAngleOfKickFromApproach(car, kickPlan);
        double rendezvousCorrection = SteerUtil.getCorrectionAngleRad(car, carPositionAtIntercept);

        Vector2 steerTarget = carPositionAtIntercept.flatten();
        SteerPlan circleTurnPlan = null;



        if (interceptModifier == null) {
            interceptModifier = kickPlan.plannedKickForce.scaledToMagnitude(-1.4);
        }

        if (carPositionAtIntercept.z > 2 && Math.abs(estimatedAngleOfKickFromApproach) < Math.PI / 12 && Math.abs(rendezvousCorrection) < Math.PI / 12) {

            plan = new Plan().withStep(new InterceptStep(interceptModifier));
            plan.begin();
            return plan.getOutput(input);
        }

        if (Math.abs(estimatedAngleOfKickFromApproach) < MAX_NOSE_HIT_ANGLE) {
            maneuverSeconds = 0;
            circleTurnPlan = new SteerPlan(SteerUtil.steerTowardGroundPosition(car, steerTarget), steerTarget);
        } else {

            Vector2 circleTerminus = steerTarget.minus(strikeForceFlat.scaled(circleBackoff));
            double correctionNeeded = estimatedAngleOfKickFromApproach - (MAX_NOSE_HIT_ANGLE * Math.signum(estimatedAngleOfKickFromApproach));
            maneuverSeconds = correctionNeeded * MANEUVER_SECONDS_PER_RADIAN;
            Vector2 terminusFacing = VectorUtil.rotateVector(carToIntercept, correctionNeeded).normalized();

            // Line up for a nose hit
            circleTurnPlan = SteerUtil.getPlanForCircleTurn(car, kickPlan.distancePlot, circleTerminus, terminusFacing);
            if (ArenaModel.getDistanceFromWall(new Vector3(circleTurnPlan.waypoint.x, circleTurnPlan.waypoint.y, 0)) < -1) {
                BotLog.println("Failing nose hit because waypoint is out of bounds", input.team);
                return Optional.empty();
            }
        }

        return getNavigation(input, circleTurnPlan);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip toward nose hit", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
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
        return Plan.concatSituation("Directed nose hit", plan);
    }
}
