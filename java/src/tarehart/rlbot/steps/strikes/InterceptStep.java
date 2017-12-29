package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.*;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static tarehart.rlbot.tuning.BotLog.println;

public class InterceptStep implements Step {
    public static final StrikeProfile AERIAL_STRIKE_PROFILE = new StrikeProfile(0, 0, 0, StrikeProfile.Style.AERIAL);
    public static final StrikeProfile FLIP_HIT_STRIKE_PROFILE = new StrikeProfile(0, 10, .3, StrikeProfile.Style.FLIP_HIT);
    public static final double PROBABLY_TOUCHING_THRESHOLD = 5.5;
    private Plan plan;
    private Vector3 interceptModifier;
    private GameTime doneMoment;
    private Intercept originalIntercept;
    private Intercept chosenIntercept;
    private BiPredicate<CarData, SpaceTime> interceptPredicate;

    public InterceptStep(Vector3 interceptModifier) {
        this(interceptModifier, (c, st) -> true);
    }

    public InterceptStep(Vector3 interceptModifier, BiPredicate<CarData, SpaceTime> predicate) {
        this.interceptModifier = interceptModifier;
        this.interceptPredicate = predicate;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (doneMoment != null && input.time.isAfter(doneMoment)) {
            println("Probably intercepted successfully", input.playerIndex);
            return Optional.empty();
        }

        CarData carData = input.getMyCarData();

        double distanceFromBall = carData.position.distance(input.ballPosition);
        if (doneMoment == null && distanceFromBall < PROBABLY_TOUCHING_THRESHOLD) {
            // You get a tiny bit more time
            doneMoment = input.time.plus(Duration.ofMillis(1000));
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(7), carData.boost, 0);

        Optional<Intercept> soonestInterceptOption = getSoonestIntercept(carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate);
        if (!soonestInterceptOption.isPresent()) {
            println("No intercept option found, aborting.", input.playerIndex);
            return Optional.empty();
        }
        chosenIntercept = soonestInterceptOption.get();

        Optional<Plan> launchPlan = StrikePlanner.planImmediateLaunch(input.getMyCarData(), chosenIntercept);
        if (launchPlan.isPresent()) {
            plan = launchPlan.get();
            plan.unstoppable();
            return plan.getOutput(input);
        }

        if (originalIntercept == null) {
            originalIntercept = chosenIntercept;
        } else {
            if (Duration.between(originalIntercept.getTime(), chosenIntercept.getTime()).getSeconds() > 3 && distanceFromBall > PROBABLY_TOUCHING_THRESHOLD) {
                if (doneMoment != null) {
                    println("Probably intercepted successfully", input.playerIndex);
                } else {
                    println("Failed to make the intercept", input.playerIndex);
                }
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }


        return Optional.of(getThereOnTime(input, chosenIntercept));
    }

    public static Optional<Intercept> getSoonestIntercept(
            CarData carData,
            BallPath ballPath,
            DistancePlot fullAcceleration,
            Vector3 interceptModifier,
            BiPredicate<CarData, SpaceTime> interceptPredicate) {
        List<Intercept> interceptOptions = new ArrayList<>();
        getAerialIntercept(carData, ballPath, interceptModifier, interceptPredicate).ifPresent(interceptOptions::add);
        getJumpHitIntercept(carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate).ifPresent(interceptOptions::add);
        getFlipHitIntercept(carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate).ifPresent(interceptOptions::add);

        return interceptOptions.stream().sorted(Comparator.comparing(Intercept::getTime)).findFirst();
    }

    private static Optional<Intercept> getAerialIntercept(CarData carData, BallPath ballPath, Vector3 interceptModifier, BiPredicate<CarData, SpaceTime> interceptPredicate) {
        if (carData.boost >= AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL) {

            double distance = carData.position.flatten().distance(ballPath.getStartPoint().space.flatten());
            Vector3 averageNoseVector = ballPath.getMotionAt(carData.time.plusSeconds(distance * .02)).get().space.minus(carData.position).normaliseCopy();

            DistancePlot budgetAcceleration = AccelerationModel.simulateAirAcceleration(carData, Duration.ofSeconds(4), averageNoseVector.flatten().magnitude());

            return InterceptCalculator.getFilteredInterceptOpportunity(carData, ballPath, budgetAcceleration, interceptModifier,
                    interceptPredicate.and(AirTouchPlanner::isVerticallyAccessible), (space) -> AERIAL_STRIKE_PROFILE);
        }
        return Optional.empty();
    }

    private static Optional<Intercept> getJumpHitIntercept(CarData carData, BallPath ballPath, DistancePlot fullAcceleration, Vector3 interceptModifier, BiPredicate<CarData, SpaceTime> interceptPredicate) {
        return InterceptCalculator.getFilteredInterceptOpportunity(
                carData, ballPath, fullAcceleration, interceptModifier, interceptPredicate.and(AirTouchPlanner::isJumpHitAccessible), AirTouchPlanner::getJumpHitStrikeProfile);
    }

    private static Optional<Intercept> getFlipHitIntercept(CarData carData, BallPath ballPath, DistancePlot fullAcceleration, Vector3 interceptModifier, BiPredicate<CarData, SpaceTime> interceptPredicate) {
        return InterceptCalculator.getFilteredInterceptOpportunity(carData, ballPath, fullAcceleration, interceptModifier,
                interceptPredicate.and(AirTouchPlanner::isFlipHitAccessible), (space) -> FLIP_HIT_STRIKE_PROFILE);
    }

    private AgentOutput getThereOnTime(AgentInput input, Intercept intercept) {

        Optional<AgentOutput> flipOut = Optional.empty();
        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, intercept.getSpace());
        if (sensibleFlip.isPresent()) {
            println("Front flip toward intercept", input.playerIndex);
            this.plan = sensibleFlip.get();
            flipOut = this.plan.getOutput(input);
        }

        if (flipOut.isPresent()) {
            return flipOut.get();
        }

        Optional<DistanceTimeSpeed> motionAfterStrike = intercept.getDistancePlot()
                .getMotionAfterDuration(car, intercept.getSpace(), Duration.between(car.time, intercept.getTime()), intercept.getStrikeProfile());

        if (motionAfterStrike.isPresent()) {
            double maxDistance = motionAfterStrike.get().distance;
            double pace = maxDistance / car.position.flatten().distance(intercept.getSpace().flatten());

            AgentOutput agentOutput = SteerUtil.steerTowardGroundPosition(car, intercept.getSpace().flatten(), car.boost <= intercept.getAirBoost());
            if (pace > 1.1) {
                // Slow down
                agentOutput.withAcceleration(0).withBoost(false).withDeceleration(Math.max(0, pace - 1.5)); // Hit the brakes, but keep steering!
                if (car.orientation.noseVector.dotProduct(car.velocity) < 0) {
                    // car is going backwards
                    agentOutput.withDeceleration(0).withSteer(0);
                }
            }
            return agentOutput;
        } else {
            AgentOutput output = SteerUtil.getThereOnTime(car, chosenIntercept.toSpaceTime());
            if (car.boost <= intercept.getAirBoost() + 5) {
                output.withBoost(false);
            }
            return output;
        }
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Working on intercept", plan);
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {

        if (Plan.activePlan(plan).isPresent()) {
            plan.getCurrentStep().drawDebugInfo(graphics);
        }

        if (chosenIntercept != null) {
            graphics.setColor(new Color(214, 136, 29));
            graphics.setStroke(new BasicStroke(1));
            Vector2 preStrike = chosenIntercept.getSpace().flatten();
            int crossSize = 2;
            graphics.draw(new Line2D.Double(preStrike.x - crossSize, preStrike.y - crossSize, preStrike.x + crossSize, preStrike.y + crossSize));
            graphics.draw(new Line2D.Double(preStrike.x - crossSize, preStrike.y + crossSize, preStrike.x + crossSize, preStrike.y - crossSize));
        }
    }
}
