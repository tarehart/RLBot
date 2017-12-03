package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.awt.geom.Line2D;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class InterceptStep implements Step {
    public static final StrikeProfile AERIAL_STRIKE_PROFILE = new StrikeProfile(0, 0, 0);
    public static final StrikeProfile JUMP_HIT_STRIKE_PROFILE = new StrikeProfile(0, 10, .3);
    public static final StrikeProfile FLIP_HIT_STRIKE_PROFILE = new StrikeProfile(0, 10, .3);
    public static final double PROBABLY_TOUCHING_THRESHOLD = 5.5;
    private Plan plan;
    private Vector3 interceptModifier;
    private LocalDateTime doneMoment;
    private Intercept originalIntercept;
    private SpaceTime preStrikePosition;

    public InterceptStep(Vector3 interceptModifier) {
        this.interceptModifier = interceptModifier;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (doneMoment != null && input.time.isAfter(doneMoment)) {
            return Optional.empty();
        }

        CarData carData = input.getMyCarData();

        double distanceFromBall = carData.position.distance(input.ballPosition);
        if (doneMoment == null && distanceFromBall < PROBABLY_TOUCHING_THRESHOLD) {
            // You get a tiny bit more time
            doneMoment = input.time.plus(Duration.ofMillis(1000));
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), carData.boost, 0);

        Optional<Intercept> chosenIntercept = getSoonestIntercept(carData, ballPath, fullAcceleration, interceptModifier);
        Optional<Plan> launchPlan = chosenIntercept.flatMap(cept -> InterceptPlanner.planImmediateLaunch(input.getMyCarData(), cept.toSpaceTime()));
        if (launchPlan.isPresent()) {
            plan = launchPlan.get();
            plan.unstoppable();
            return plan.getOutput(input);
        }

        if (chosenIntercept.isPresent()) {
            if (originalIntercept == null) {
                originalIntercept = chosenIntercept.get();
            } else {
                if (TimeUtil.secondsBetween(originalIntercept.getTime(), chosenIntercept.get().getTime()) > 3 && distanceFromBall > PROBABLY_TOUCHING_THRESHOLD) {
                    if (doneMoment != null) {
                        println("Probably intercepted successfully", input.playerIndex);
                    } else {
                        println("Failed to make the intercept", input.playerIndex);
                    }
                    return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
                }
            }
        }


        return chosenIntercept.map(intercept -> getThereOnTime(input, intercept));
    }

    public static Optional<Intercept> getSoonestIntercept(CarData carData, BallPath ballPath, DistancePlot fullAcceleration, Vector3 interceptModifier) {
        List<Intercept> interceptOptions = new ArrayList<>();
        getAerialIntercept(carData, ballPath, interceptModifier).ifPresent(interceptOptions::add);
        getJumpHitIntercept(carData, ballPath, fullAcceleration, interceptModifier).ifPresent(interceptOptions::add);
        getFlipHitIntercept(carData, ballPath, fullAcceleration, interceptModifier).ifPresent(interceptOptions::add);

        return interceptOptions.stream().sorted(Comparator.comparing(Intercept::getTime)).findFirst();
    }

    private static Optional<Intercept> getAerialIntercept(CarData carData, BallPath ballPath, Vector3 interceptModifier) {
        if (carData.boost >= AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL) {
            DistancePlot budgetAcceleration = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), AirTouchPlanner.getBoostBudget(carData), 0);
            Optional<SpaceTime> budgetInterceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(carData, ballPath, budgetAcceleration, interceptModifier, AirTouchPlanner::isVerticallyAccessible, AERIAL_STRIKE_PROFILE);
            if (budgetInterceptOpportunity.isPresent()) {
                SpaceTime spaceTime = budgetInterceptOpportunity.get();
                if (budgetInterceptOpportunity.get().space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
                    return Optional.of(new Intercept(spaceTime.space, spaceTime.time, AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL, AERIAL_STRIKE_PROFILE, budgetAcceleration));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Intercept> getJumpHitIntercept(CarData carData, BallPath ballPath, DistancePlot fullAcceleration, Vector3 interceptModifier) {
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(carData, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isJumpHitAccessible, JUMP_HIT_STRIKE_PROFILE);
        if (interceptOpportunity.isPresent()) {
            if (interceptOpportunity.get().space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
                return Optional.of(new Intercept(interceptOpportunity.get(), JUMP_HIT_STRIKE_PROFILE, fullAcceleration));
            }
        }
        return Optional.empty();
    }

    private static Optional<Intercept> getFlipHitIntercept(CarData carData, BallPath ballPath, DistancePlot fullAcceleration, Vector3 interceptModifier) {
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(carData, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isFlipHitAccessible, FLIP_HIT_STRIKE_PROFILE);
        return interceptOpportunity.map(spaceTime -> new Intercept(spaceTime, FLIP_HIT_STRIKE_PROFILE, fullAcceleration));
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

        Optional<DistanceTimeSpeed> motionAfterStrike = intercept.getDistancePlot().getMotionAfterStrike(car, intercept.toSpaceTime(), intercept.getStrikeProfile());

        if (motionAfterStrike.isPresent()) {
            double maxDistance = motionAfterStrike.get().distance;
            double pace = maxDistance / car.position.flatten().distance(intercept.getSpace().flatten());

            AgentOutput agentOutput = SteerUtil.steerTowardGroundPosition(car, intercept.getSpace().flatten(), car.boost <= intercept.getAirBoost());
            if (pace > 1.1) {
                // Slow down
                agentOutput.withAcceleration(0).withBoost(false).withDeceleration(Math.max(0, pace - 1.5)); // Hit the brakes, but keep steering!
            }
            return agentOutput;
        } else {
            AgentOutput output = SteerUtil.getThereOnTime(car, preStrikePosition);
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
            return;
        }

        if (preStrikePosition != null) {
            graphics.setColor(new Color(214, 136, 29));
            graphics.setStroke(new BasicStroke(1));
            Vector2 preStrike = preStrikePosition.space.flatten();
            int crossSize = 2;
            graphics.draw(new Line2D.Double(preStrike.x - crossSize, preStrike.y - crossSize, preStrike.x + crossSize, preStrike.y + crossSize));
            graphics.draw(new Line2D.Double(preStrike.x - crossSize, preStrike.y + crossSize, preStrike.x + crossSize, preStrike.y - crossSize));
        }
    }
}
