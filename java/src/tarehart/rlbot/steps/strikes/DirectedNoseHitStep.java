package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.travel.SlideToPositionStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;
import tarehart.rlbot.tuning.BotLog;

import java.awt.*;
import java.util.Optional;

import static java.util.Optional.empty;
import static tarehart.rlbot.tuning.BotLog.println;

public class DirectedNoseHitStep implements Step {
    public static final double MAX_NOSE_HIT_ANGLE = Math.PI / 18;
    private Plan plan;
    private Vector3 originalIntercept;
    private GameTime doneMoment;
    private KickStrategy kickStrategy;
    private Vector3 interceptModifier = null;
    private double worstCaseManeuverSeconds = 0;
    private double estimatedAngleOfKickFromApproach;
    private SteerPlan circleTurnPlan;
    private DirectedKickPlan kickPlan;
    private CarData car;

    public DirectedNoseHitStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public static boolean canMakeDirectedKick(AgentInput input, KickStrategy kickStrategy) {
        boolean tooBouncy = BallPhysics.getGroundBounceEnergy(input.ballPosition.z, input.ballVelocity.z) > 30;

        Vector2 kickDirection = kickStrategy.getKickDirection(input).flatten();
        Vector2 carToBall = input.ballPosition.minus(input.getMyCarData().position).flatten();

        double angle = Vector2.angle(kickDirection, carToBall);

        boolean wrongSide = angle > Math.PI * 2 / 3;

        return !tooBouncy && !wrongSide;
    }

    public double getEstimatedAngleOfKickFromApproach() {
        return estimatedAngleOfKickFromApproach;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        car = input.getMyCarData();

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

            StrikeProfile strikeProfile = new StrikeProfile(worstCaseManeuverSeconds, 0, 0);
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, false, interceptModifier, strikeProfile);
        } else {
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, false);
        }

        if (!kickPlanOption.isPresent()) {
            BotLog.println("Quitting nose hit due to failed kick plan.", car.playerIndex);
            return Optional.empty();
        }

        kickPlan = kickPlanOption.get();

        if (originalIntercept == null) {
            originalIntercept = kickPlan.ballAtIntercept.getSpace();
        } else {
            if (originalIntercept.distance(kickPlan.ballAtIntercept.getSpace()) > 30) {
                println("Failed to make the nose hit", input.playerIndex);
                return empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }


        Double circleBackoff = 3 + kickPlan.ballAtIntercept.getSpace().z * 3; // Take more of a straight runway if the ball is high up.


        Vector2 strikeForceFlat = kickPlan.plannedKickForce.flatten().normalized();
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();
        Vector2 carToIntercept = carPositionAtIntercept.minus(car.position).flatten();
        Vector2 carPositionAtInterceptFlat = carPositionAtIntercept.flatten();
        Vector2 circleTerminus = carPositionAtInterceptFlat.minus(strikeForceFlat.scaled(circleBackoff));
        estimatedAngleOfKickFromApproach = getAngleOfKickFromApproach(car, kickPlan, circleTerminus);
        double rendezvousCorrection = SteerUtil.getCorrectionAngleRad(car, circleTerminus);

        circleTurnPlan = null;


        if (interceptModifier == null) {
            interceptModifier = kickPlan.plannedKickForce.scaledToMagnitude(-1.4);
        }

        if (carPositionAtIntercept.z > 2 && Math.abs(estimatedAngleOfKickFromApproach) < Math.PI / 20 && Math.abs(rendezvousCorrection) < Math.PI / 20) {
            circleTurnPlan = null;
            plan = new Plan().withStep(new InterceptStep(interceptModifier));
            return plan.getOutput(input);
        }

        if (Math.abs(estimatedAngleOfKickFromApproach) < MAX_NOSE_HIT_ANGLE) {
            circleTurnPlan = new SteerPlan(SteerUtil.steerTowardGroundPosition(car, carPositionAtInterceptFlat), carPositionAtInterceptFlat);
        } else {

            double correctionNeeded = Math.max(0, Math.abs(estimatedAngleOfKickFromApproach) - (MAX_NOSE_HIT_ANGLE * Math.signum(estimatedAngleOfKickFromApproach)));
            double freshManeuverSeconds = getManeuverSeconds(correctionNeeded, car);
            boolean maneuverSecondsStable = Math.abs(worstCaseManeuverSeconds - freshManeuverSeconds) < .1;
            worstCaseManeuverSeconds = Math.max(worstCaseManeuverSeconds, freshManeuverSeconds);


            if (maneuverSecondsStable && Vector2.angle(carToIntercept, strikeForceFlat) > 2 * Math.PI / 3) {
                // If we're planning to turn a huge amount, this is a waste of time.
                return Optional.empty();
            }

            double secondsTillIntercept = Duration.between(input.time, kickPlan.ballAtIntercept.getTime()).getSeconds();

            double asapSeconds = kickPlan.distancePlot
                    .getMotionUponArrival(
                        car,
                        kickPlan.ballAtIntercept.getSpace(),
                        new StrikeProfile(freshManeuverSeconds, 10, .3))
                    .map(DistanceTimeSpeed::getTime)
                    .orElse(secondsTillIntercept);

            if (secondsTillIntercept > asapSeconds + .5) {
                plan = new Plan(Plan.Posture.OFFENSIVE)
                        .withStep(new SlideToPositionStep((in) -> new PositionFacing(circleTerminus, strikeForceFlat)));
                return plan.getOutput(input);
            }

            // Line up for a nose hit
            circleTurnPlan = SteerUtil.getPlanForCircleTurn(car, kickPlan.distancePlot, circleTerminus, strikeForceFlat);
            if (ArenaModel.getDistanceFromWall(new Vector3(circleTurnPlan.waypoint.x, circleTurnPlan.waypoint.y, 0)) < -1) {
                println("Failing nose hit because waypoint is out of bounds", input.playerIndex);
                return empty();
            }
        }

        return getNavigation(input, circleTurnPlan);
    }

    private double getManeuverSeconds(double curveRideAngle, CarData car) {
        double seconds = curveRideAngle * .1;
        if (circleTurnPlan != null) {
            double noseToWaypoint = Vector2.angle(circleTurnPlan.waypoint.minus(car.position.flatten()), car.orientation.noseVector.flatten());
            seconds += noseToWaypoint * .4;
        }
        return seconds;
    }

    private static double getAngleOfKickFromApproach(CarData car, DirectedKickPlan kickPlan, Vector2 launchPad) {
        Vector2 strikeForceFlat = kickPlan.plannedKickForce.flatten();
        Vector2 carToLaunchPad = launchPad.minus(car.position.flatten());
        return carToLaunchPad.correctionAngle(strikeForceFlat);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint);
        if (sensibleFlip.isPresent()) {
            println("Front flip toward nose hit", input.playerIndex);
            this.plan = sensibleFlip.get();
            return this.plan.getOutput(input);
        }

        return Optional.of(circleTurnOption.immediateSteer);
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Directed nose hit", plan);
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {

        if (Plan.activePlan(plan).isPresent()) {
            plan.getCurrentStep().drawDebugInfo(graphics);
            return;
        }

        if (circleTurnPlan != null) {
            graphics.setColor(new Color(138, 164, 200));
            circleTurnPlan.drawDebugInfo(graphics, car);
        }

        if (kickPlan != null) {
            kickPlan.drawDebugInfo(graphics);
        }
    }
}
