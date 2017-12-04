package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.tuning.ManeuverMath;
import tarehart.rlbot.ui.ArenaDisplay;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static tarehart.rlbot.planning.SteerUtil.getCorrectionAngleRad;
import static tarehart.rlbot.planning.SteerUtil.steerTowardGroundPosition;
import static tarehart.rlbot.tuning.BotLog.println;

public class DirectedSideHitStep implements Step {
    private static final double MANEUVER_SECONDS_PER_RADIAN = .1;
    private static final double GAP_BEFORE_DODGE = 1.5;
    private static final double DISTANCE_AT_CONTACT = 1.8;
    private Plan plan;
    private Vector3 originalIntercept;
    private LocalDateTime doneMoment;
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
            BotLog.println("Quitting side hit due to failed kick plan.", car.playerIndex);
            return Optional.empty();
        }

        kickPlan = kickPlanOption.get();

        if (input.getEnemyCarData()
                .map(enemy -> TacticsAdvisor.calculateRaceResult(kickPlan.ballAtIntercept.toSpaceTime(), enemy, kickPlan.ballPath) < -0.5)
                .orElse(false)) {
            BotLog.println("Failing side hit because we will lose the race.", car.playerIndex);
            return Optional.empty();
        }

        if (interceptModifier == null) {
            Vector3 nearSide = kickPlan.plannedKickForce.scaledToMagnitude(-(DISTANCE_AT_CONTACT + GAP_BEFORE_DODGE));
            interceptModifier = new Vector3(nearSide.x, nearSide.y, nearSide.z - 1.4); // Closer to ground
        }

        if (originalIntercept == null) {
            originalIntercept = kickPlan.ballAtIntercept.getSpace();
        } else {
            if (originalIntercept.distance(kickPlan.ballAtIntercept.getSpace()) > 30) {
                println("Failed to make the directed kick", input.playerIndex);
                return empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        Vector2 strikeDirection = kickPlan.plannedKickForce.flatten().normalized();
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();

        Vector2 orthogonalPoint = carPositionAtIntercept.flatten();

        if (finalApproach) {
            return performFinalApproach(input, orthogonalPoint, kickPlan, carPositionAtIntercept, strikeDirection);
        }

        Optional<Double> strikeTime = getStrikeTime(carPositionAtIntercept, GAP_BEFORE_DODGE);
        if (!strikeTime.isPresent()) {
            return Optional.empty();
        }
        double expectedSpeed = kickPlan.distancePlot.getMotionAfterDistance(car.position.flatten().distance(orthogonalPoint)).map(m -> m.speed).orElse(40.0);
        double backoff = expectedSpeed * strikeTime.get() + 1;

        if (backoff > car.position.flatten().distance(orthogonalPoint)) {
            BotLog.println("Failed the side hit.", car.playerIndex);
            return Optional.empty();
        }

        Vector2 carToIntercept = carPositionAtIntercept.minus(car.position).flatten();
        Vector2 facingForSideFlip = VectorUtil.orthogonal(strikeDirection, v -> v.dotProduct(carToIntercept) > 0).normalized();

        Vector2 steerTarget = orthogonalPoint.minus(facingForSideFlip.scaled(backoff));

        Vector2 toOrthogonal = orthogonalPoint.minus(car.position.flatten());

        double distance = toOrthogonal.magnitude();
        Vector2 carNose = car.orientation.noseVector.flatten();
        double angle = Vector2.angle(carNose, facingForSideFlip);
        if (distance < backoff + 3 && angle < Math.PI / 8) {
            doneMoment = input.time.plus(TimeUtil.toDuration(strikeTime.get() + .5));
            finalApproach = true;
            maneuverSeconds = 0;
            circleTurnPlan = null;
            // Done with the circle turn. Drive toward the orthogonal point and wait for the right moment to launch.
            return performFinalApproach(input, orthogonalPoint, kickPlan, carPositionAtIntercept, strikeDirection);
        }


        maneuverSeconds = angle * MANEUVER_SECONDS_PER_RADIAN;

        circleTurnPlan = SteerUtil.getPlanForCircleTurn(car, kickPlan.distancePlot, steerTarget, facingForSideFlip);

        return getNavigation(input, circleTurnPlan);
    }

    private Optional<Double> getStrikeTime(Vector3 carPositionAtIntercept, double approachDistance) {
        return getJumpTime(carPositionAtIntercept).map(t -> t + ManeuverMath.secondsForSideFlipTravel(approachDistance));
    }

    private Optional<AgentOutput> performFinalApproach(AgentInput input, Vector2 orthogonalPoint, DirectedKickPlan kickPlan, Vector3 carPositionAtIntercept, Vector2 strikeDirection) {

        // You're probably darn close to flip time.

        CarData car = input.getMyCarData();

        Optional<Double> jumpTime = getJumpTime(carPositionAtIntercept);
        if (!jumpTime.isPresent()) {
            return Optional.empty();
        }
        Vector2 carAtImpact = kickPlan.ballAtIntercept.space.flatten().plus(strikeDirection.scaled(-DISTANCE_AT_CONTACT));
        Vector2 toImpact = carAtImpact.minus(car.position.flatten());
        Vector2 projectedApproach = VectorUtil.project(toImpact, car.orientation.rightVector.flatten());
        double realApproachDistance = projectedApproach.magnitude();
        Optional<Double> strikeTime = getStrikeTime(carPositionAtIntercept, realApproachDistance);
        if (!strikeTime.isPresent()) {
            return Optional.empty();
        }
        double backoff = car.velocity.magnitude() * strikeTime.get();

        double distance = car.position.flatten().distance(orthogonalPoint);
        if (distance < backoff) {
            // Time to launch!
            double strikeForceCorrection = DirectedKickUtil.getAngleOfKickFromApproach(car, kickPlan);
            plan = SetPieces.jumpSideFlip(strikeForceCorrection > 0, jumpTime.get());
            return plan.getOutput(input);
        } else {
            println(format("Side flip soon. Distance: %.2f", distance), input.playerIndex);
            return of(steerTowardGroundPosition(car, orthogonalPoint));
        }
    }

    private Optional<Double> getJumpTime(Vector3 carPositionAtIntercept) {
        return ManeuverMath.secondsForMashJumpHeight(carPositionAtIntercept.z);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        if (car.boost == 0) {
            Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint);
            if (sensibleFlip.isPresent()) {
                println("Front flip toward side hit", input.playerIndex);
                this.plan = sensibleFlip.get();
                return this.plan.getOutput(input);
            }
        }

        return Optional.of(circleTurnOption.immediateSteer);
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
