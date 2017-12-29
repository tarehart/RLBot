package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.AirTouchPlanner;
import tarehart.rlbot.intercept.StrikeProfile;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.routing.CircleTurnUtil;
import tarehart.rlbot.routing.SteerPlan;
import tarehart.rlbot.steps.Step;
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
    private double carLaunchpadInterceptAngle;
    private SteerPlan circleTurnPlan;
    private DirectedKickPlan kickPlan;
    private CarData car;
    private GameTime earliestPossibleIntercept;

    public DirectedNoseHitStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public static boolean canMakeDirectedKick(AgentInput input, KickStrategy kickStrategy) {
        boolean tooBouncy = BallPhysics.getGroundBounceEnergy(input.getBallPosition().getZ(), input.getBallVelocity().getZ()) > 30;

        Vector2 kickDirection = kickStrategy.getKickDirection(input).flatten();
        Vector2 carToBall = input.getBallPosition().minus(input.getMyCarData().getPosition()).flatten();

        double angle = Vector2.Companion.angle(kickDirection, carToBall);

        boolean wrongSide = angle > Math.PI * 2 / 3;

        return !tooBouncy && !wrongSide;
    }

    public double getCarLaunchpadInterceptAngle() {
        return carLaunchpadInterceptAngle;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        car = input.getMyCarData();

        if (doneMoment == null && car.getPosition().distance(input.getBallPosition()) < 4.5) {
            // You get a tiny bit more time
            doneMoment = input.getTime().plus(Duration.ofMillis(200));
        }

        if (earliestPossibleIntercept == null) {
            earliestPossibleIntercept = input.getTime();
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (doneMoment != null && input.getTime().isAfter(doneMoment)) {
            return Optional.empty();
        }

        if (ArenaModel.isCarOnWall(car)) {
            return Optional.empty();
        }

        final Optional<DirectedKickPlan> kickPlanOption;
        if (interceptModifier != null) {
            kickPlanOption = DirectedKickUtil.planKick(
                    input,
                    kickStrategy,
                    false,
                    interceptModifier,
                    (space) -> new StrikeProfile(),
                    earliestPossibleIntercept);
        } else {
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, false);
        }

        if (!kickPlanOption.isPresent()) {
            BotLog.println("Quitting nose hit due to failed kick plan.", car.getPlayerIndex());
            return Optional.empty();
        }

        kickPlan = kickPlanOption.get();

        if (originalIntercept == null) {
            originalIntercept = kickPlan.ballAtIntercept.getSpace();
        } else {
            if (originalIntercept.distance(kickPlan.ballAtIntercept.getSpace()) > 30) {
                println("Failed to make the nose hit", input.getPlayerIndex());
                return empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        Double circleBackoff = kickPlan.launchPad.distance(kickPlan.intercept.getSpace().flatten());


        Vector2 strikeForceFlat = kickPlan.plannedKickForce.flatten().normalized();
        Vector3 interceptLocation = kickPlan.intercept.getSpace();
        Vector2 carToIntercept = interceptLocation.minus(car.getPosition()).flatten();
        Vector2 interceptLocationFlat = interceptLocation.flatten();

        carLaunchpadInterceptAngle = measureCarLaunchpadInterceptAngle(car, kickPlan);
        double positionCorrectionForStrike = carToIntercept.correctionAngle(strikeForceFlat);
        double orientationCorrectionForStrike = car.getOrientation().getNoseVector().flatten().correctionAngle(strikeForceFlat);

        circleTurnPlan = null;


        if (interceptModifier == null) {
            interceptModifier = kickPlan.plannedKickForce.scaledToMagnitude(-1.4);
        }

        if (interceptLocation.getZ() > 2 && Math.abs(positionCorrectionForStrike) < Math.PI / 12 && Math.abs(orientationCorrectionForStrike) < Math.PI / 12) {
            circleTurnPlan = null;
            plan = new Plan().withStep(new InterceptStep(interceptModifier, (c, st) -> !st.time.isBefore(earliestPossibleIntercept)));
            return plan.getOutput(input);
        }


        double freshManeuverSeconds = getManeuverSeconds();
        GameTime earliestThisTime = kickPlan.intercept.getTime().plusSeconds(freshManeuverSeconds).minus(kickPlan.intercept.getSpareTime());
        double timeMismatch = Duration.between(earliestPossibleIntercept, earliestThisTime).getSeconds();

        if (Math.abs(timeMismatch) < .2 && positionCorrectionForStrike > Math.PI / 3 && carToIntercept.magnitude() > 20) {
            // If we're planning to turn a huge amount, this is a waste of time.
            return Optional.empty();
        }

        // If you're facing the intercept, but the circle backoff wants you to backtrack, you should just wait
        // for a later intercept instead.
        boolean waitForLaterIntercept = Math.abs(carLaunchpadInterceptAngle) > Math.PI / 2 && Math.abs(orientationCorrectionForStrike) < Math.PI / 2;

        if (waitForLaterIntercept) {
            earliestPossibleIntercept = earliestPossibleIntercept.plusSeconds(.5);
        } else if (kickPlan.intercept.getSpace().getZ() < AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
            earliestPossibleIntercept = earliestPossibleIntercept.plusSeconds(timeMismatch / 2);
        } else if (Math.abs(positionCorrectionForStrike) < MAX_NOSE_HIT_ANGLE) {
            circleTurnPlan = new SteerPlan(SteerUtil.steerTowardGroundPosition(car, interceptLocationFlat), interceptLocationFlat);
            return getNavigation(input, circleTurnPlan);
        }

        double asapSeconds = kickPlan.distancePlot
                .getMotionUponArrival(
                    car,
                    kickPlan.ballAtIntercept.getSpace(),
                    new StrikeProfile())
                .map(DistanceTimeSpeed::getTime)
                .orElse(Duration.between(input.getTime(), kickPlan.ballAtIntercept.getTime())).getSeconds();

//            if (secondsTillIntercept > asapSeconds + .5) {
//                plan = new Plan(Plan.Posture.OFFENSIVE)
//                        .withStep(new SlideToPositionStep((in) -> new PositionFacing(circleTerminus, strikeForceFlat)));
//                return plan.getOutput(input);
//            }

        // Line up for a nose hit
        circleTurnPlan = CircleTurnUtil.getPlanForCircleTurn(car, kickPlan.distancePlot, kickPlan.launchPad, strikeForceFlat);
        if (ArenaModel.getDistanceFromWall(new Vector3(circleTurnPlan.waypoint.getX(), circleTurnPlan.waypoint.getY(), 0)) < -1) {
            println("Failing nose hit because waypoint is out of bounds", input.getPlayerIndex());
            return empty();
        }


        return getNavigation(input, circleTurnPlan);
    }

    private double getManeuverSeconds() {
        double seconds = carLaunchpadInterceptAngle * .1;
        if (circleTurnPlan != null) {
            double noseToWaypoint = Vector2.Companion.angle(circleTurnPlan.waypoint.minus(car.getPosition().flatten()), car.getOrientation().getNoseVector().flatten());
            seconds += noseToWaypoint * .4;
        }
        return seconds;
    }

    /**
     * First you drive from where you are now to the launchpad.
     * Then you have to
     */
    private static double measureCarLaunchpadInterceptAngle(CarData car, DirectedKickPlan kickPlan) {
        Vector2 strikeForceFlat = kickPlan.plannedKickForce.flatten();
        Vector2 carToLaunchPad = kickPlan.launchPad.minus(car.getPosition().flatten());
        return carToLaunchPad.correctionAngle(strikeForceFlat);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint);
        if (sensibleFlip.isPresent()) {
            println("Front flip toward nose hit", input.getPlayerIndex());
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
