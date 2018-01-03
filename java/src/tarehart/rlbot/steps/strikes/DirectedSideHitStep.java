package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.BallTouch;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.StrikeProfile;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.routing.CircleTurnUtil;
import tarehart.rlbot.routing.SteerPlan;
import tarehart.rlbot.routing.StrikePoint;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.tuning.ManeuverMath;

import java.awt.*;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static tarehart.rlbot.planning.SteerUtil.steerTowardGroundPosition;
import static tarehart.rlbot.tuning.BotLog.println;

public class DirectedSideHitStep implements Step {
    private static final double MANEUVER_SECONDS_PER_RADIAN = .1;
    private static final double GAP_BEFORE_DODGE = 1.5;
    private static final double DISTANCE_AT_CONTACT = 2;
    private Plan plan;
    private Vector3 originalIntercept;
    private Optional<BallTouch> originalTouch;
    private GameTime doneMoment;
    private KickStrategy kickStrategy;
    private Vector3 interceptModifier = null;
    private double maneuverSeconds = 0;
    private boolean finalApproach = false;
    private SteerPlan circleTurnPlan;
    private CarData car;
    private DirectedKickPlan kickPlan;

    public DirectedSideHitStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        car = input.getMyCarData();

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (doneMoment != null && input.getTime().isAfter(doneMoment)) {
            return Optional.empty();
        }

        if (finalApproach) {
            // Freeze the kick plan
            return performFinalApproach(input, kickPlan);
        }

        final Optional<DirectedKickPlan> kickPlanOption;
        if (interceptModifier != null) {
            StrikeProfile strikeProfile = new StrikeProfile(maneuverSeconds, 0, 0, 0, StrikeProfile.Style.SIDE_HIT);
            kickPlanOption = DirectedKickUtil.INSTANCE.planKick(input, kickStrategy, true, interceptModifier, (space) -> strikeProfile, input.getTime());
        } else {
            kickPlanOption = DirectedKickUtil.INSTANCE.planKick(input, kickStrategy, true);
        }

        if (!kickPlanOption.isPresent()) {
            BotLog.println("Quitting side hit due to failed kick plan.", car.getPlayerIndex());
            return Optional.empty();
        }

        kickPlan = kickPlanOption.get();

        if (interceptModifier == null) {
            Vector3 nearSide = kickPlan.getPlannedKickForce().scaledToMagnitude(-(DISTANCE_AT_CONTACT + GAP_BEFORE_DODGE));
            interceptModifier = new Vector3(nearSide.getX(), nearSide.getY(), nearSide.getZ() - 1.4); // Closer to ground
        } else if (kickPlan.getIntercept().getSpareTime().getSeconds() > 2) {
            return Optional.empty(); // Too much time to be waiting around.
        }

        if (originalIntercept == null) {
            originalIntercept = kickPlan.getBallAtIntercept().getSpace();
            originalTouch = input.getLatestBallTouch();
        } else {
            if (originalIntercept.distance(kickPlan.getBallAtIntercept().getSpace()) > 30) {
                println("Failed to make the side kick", input.getPlayerIndex());
                return empty(); // Failed to kick it soon enough, new stuff has happened.
            }

            if (!input.getLatestBallTouch().map(BallTouch::getPosition).orElse(new Vector3())
                    .equals(originalTouch.map(BallTouch::getPosition).orElse(new Vector3())) ) {
                // There has been a new ball touch.
                println("Ball has been touched, quitting side hit", input.getPlayerIndex());
                return Optional.empty();
            }
        }

        Vector2 strikeDirection = kickPlan.getPlannedKickForce().flatten().normalized();
        Vector3 carPositionAtIntercept = kickPlan.getIntercept().getSpace();

        Vector2 orthogonalPoint = carPositionAtIntercept.flatten();

        Optional<Duration> strikeTime = getStrikeTime(carPositionAtIntercept, GAP_BEFORE_DODGE);
        if (!strikeTime.isPresent()) {
            return Optional.empty();
        }
        double expectedSpeed = kickPlan.getDistancePlot().getMotionAfterDistance(car.getPosition().flatten().distance(orthogonalPoint)).map(m -> m.getSpeed()).orElse(40.0);
        double backoff = expectedSpeed * strikeTime.get().getSeconds() + 1;

        if (backoff > car.getPosition().flatten().distance(orthogonalPoint)) {
            BotLog.println("Failed the side hit.", car.getPlayerIndex());
            return Optional.empty();
        }

        Vector2 carToIntercept = carPositionAtIntercept.minus(car.getPosition()).flatten();
        Vector2 facingForSideFlip = VectorUtil.INSTANCE.orthogonal(strikeDirection, v -> v.dotProduct(carToIntercept) > 0).normalized();

        if (Vector2.Companion.angle(carToIntercept, facingForSideFlip) > Math.PI / 3) {
            // If we're doing more than a quarter turn, this is a waste of time.
            return Optional.empty();
        }

        Vector2 steerTarget = orthogonalPoint.minus(facingForSideFlip.scaled(backoff));
        StrikePoint strikePoint = new StrikePoint(steerTarget, facingForSideFlip, kickPlan.getIntercept().getTime().minus(strikeTime.get()));

        Vector2 toOrthogonal = orthogonalPoint.minus(car.getPosition().flatten());

        double distance = toOrthogonal.magnitude();
        Vector2 carNose = car.getOrientation().getNoseVector().flatten();
        double angle = Vector2.Companion.angle(carNose, facingForSideFlip);
        double positionCorrectionForStrike = carToIntercept.correctionAngle(facingForSideFlip);
        if (distance < backoff + 3 && angle < Math.PI / 12 && Math.abs(positionCorrectionForStrike) < Math.PI / 12 && !ManeuverMath.isSkidding(car)) {
            doneMoment = kickPlan.getIntercept().getTime().plusSeconds(.5);
            finalApproach = true;
            maneuverSeconds = 0;
            circleTurnPlan = null;
            // Done with the circle turn. Drive toward the orthogonal point and wait for the right moment to launch.
            return performFinalApproach(input, kickPlan);
        }


        maneuverSeconds = angle * MANEUVER_SECONDS_PER_RADIAN;

        circleTurnPlan = CircleTurnUtil.getPlanForCircleTurn(car, kickPlan.getDistancePlot(), strikePoint);

        return getNavigation(input, circleTurnPlan);
    }

    private Optional<Duration> getStrikeTime(Vector3 carPositionAtIntercept, double approachDistance) {
        return getJumpTime(carPositionAtIntercept).map(t -> t.plusSeconds(ManeuverMath.secondsForSideFlipTravel(approachDistance)));
    }

    private Optional<AgentOutput> performFinalApproach(AgentInput input, DirectedKickPlan kickPlan) {

        // You're probably darn close to flip time.

        Vector2 strikeDirection = kickPlan.getPlannedKickForce().flatten().normalized();
        Vector3 carPositionAtIntercept = kickPlan.getIntercept().getSpace();

        Vector2 orthogonalPoint = carPositionAtIntercept.flatten();

        CarData car = input.getMyCarData();

        Optional<Duration> jumpTime = getJumpTime(carPositionAtIntercept);
        if (!jumpTime.isPresent()) {
            return Optional.empty();
        }
        Vector2 carAtImpact = kickPlan.getBallAtIntercept().getSpace().flatten().plus(strikeDirection.scaled(-DISTANCE_AT_CONTACT));
        Vector2 toImpact = carAtImpact.minus(car.getPosition().flatten());
        Vector2 projectedApproach = VectorUtil.INSTANCE.project(toImpact, car.getOrientation().getRightVector().flatten());
        double realApproachDistance = projectedApproach.magnitude();
        Optional<Duration> strikeTime = getStrikeTime(carPositionAtIntercept, realApproachDistance);
        if (!strikeTime.isPresent()) {
            return Optional.empty();
        }
        double strikeSeconds = strikeTime.get().getSeconds();
        double backoff = car.getVelocity().magnitude() * strikeSeconds;

        double distance = car.getPosition().flatten().distance(orthogonalPoint);
        double distanceCountdown = distance - backoff;
        double timeCountdown = Duration.Companion.between(car.getTime().plus(strikeTime.get()), kickPlan.getIntercept().getTime()).getSeconds();
        if (distanceCountdown < .1 && timeCountdown < .1) {
            // Time to launch!
            double strikeForceCorrection = DirectedKickUtil.INSTANCE.getAngleOfKickFromApproach(car, kickPlan);
            plan = SetPieces.jumpSideFlip(strikeForceCorrection > 0, jumpTime.get(), false);
            return plan.getOutput(input);
        } else {
            println(format("Side flip soon. Distance: %.2f, Time: %.2f", distanceCountdown, timeCountdown), input.getPlayerIndex());
            return of(SteerUtil.getThereOnTime(car, new SpaceTime(orthogonalPoint.toVector3(), kickPlan.getIntercept().getTime())));
        }
    }

    private Optional<Duration> getJumpTime(Vector3 carPositionAtIntercept) {
        return ManeuverMath.secondsForMashJumpHeight(carPositionAtIntercept.getZ()).map(Duration.Companion::ofSeconds);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        if (car.getBoost() == 0) {
            Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.getWaypoint());
            if (sensibleFlip.isPresent()) {
                println("Front flip toward side hit", input.getPlayerIndex());
                this.plan = sensibleFlip.get();
                return this.plan.getOutput(input);
            }
        }

        return Optional.of(circleTurnOption.getImmediateSteer());
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Directed Side Hit", plan);
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {

        if (Plan.activePlan(plan).isPresent()) {
            plan.getCurrentStep().drawDebugInfo(graphics);
            return;
        }

        if (circleTurnPlan != null) {
            graphics.setColor(new Color(190, 129, 200));
            circleTurnPlan.drawDebugInfo(graphics, car);
        }

        if (kickPlan != null) {
            kickPlan.drawDebugInfo(graphics);
        }
    }
}
