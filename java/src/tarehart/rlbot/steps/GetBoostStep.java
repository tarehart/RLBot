package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.FullBoost;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.routing.CircleTurnUtil;
import tarehart.rlbot.routing.SteerPlan;
import tarehart.rlbot.time.Duration;

import java.awt.*;
import java.util.List;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class GetBoostStep implements Step {
    private FullBoost targetLocation = null;
    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (targetLocation == null) {
            init(input);
        }

        Optional<FullBoost> matchingBoost = input.fullBoosts.stream().filter(b -> b.location.distance(targetLocation.location) < 1).findFirst();
        if (!matchingBoost.isPresent()) {
            return Optional.empty();
        }

        targetLocation = matchingBoost.get();

        if (!targetLocation.isActive) {
            return Optional.empty();
        }

        CarData car = input.getMyCarData();

        double distance = SteerUtil.getDistanceFromCar(car, targetLocation.location);

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (distance < 3) {
            return Optional.empty();
        } else {

            CarData carData = input.getMyCarData();
            Vector2 myPosition = carData.position.flatten();
            Vector3 target = targetLocation.location;
            Vector2 toBoost = target.flatten().minus(myPosition);


            DistancePlot distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost);
            Vector2 facing = VectorUtil.orthogonal(target.flatten(), v -> v.dotProduct(toBoost) > 0).normalized();

            SteerPlan planForCircleTurn = CircleTurnUtil.getPlanForCircleTurn(car, distancePlot, target.flatten(), facing);

            Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, planForCircleTurn.waypoint);
            if (sensibleFlip.isPresent()) {
                println("Flipping toward boost", input.playerIndex);
                plan = sensibleFlip.get();
                return plan.getOutput(input);
            }

            return Optional.of(planForCircleTurn.immediateSteer);
        }
    }

    private void init(AgentInput input) {
        targetLocation = getTacticalBoostLocation(input);
    }

    private static FullBoost getTacticalBoostLocation(AgentInput input) {
        FullBoost nearestLocation = null;
        double minTime = Double.MAX_VALUE;
        CarData carData = input.getMyCarData();
        DistancePlot distancePlot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), carData.boost);
        for (FullBoost boost : input.fullBoosts) {
            Optional<Duration> travelSeconds = AccelerationModel.getTravelSeconds(carData, distancePlot, boost.location);
            if (travelSeconds.isPresent() && travelSeconds.get().getSeconds() < minTime &&
                    (boost.isActive || travelSeconds.get().minus(Duration.between(input.time, boost.activeTime)).getSeconds() > 1)) {

                minTime = travelSeconds.get().getSeconds();
                nearestLocation = boost;
            }
        }
        if (minTime < 1.5) {
            return nearestLocation;
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);
        Vector3 endpoint = ballPath.getEndpoint().getSpace();
        // Add a defensive bias.
        Vector3 idealPlaceToGetBoost = new Vector3(endpoint.getX(), 40 * Math.signum(GoalUtil.getOwnGoal(input.team).getCenter().getY()), 0);
        return getNearestBoost(input.fullBoosts, idealPlaceToGetBoost);
    }

    private static FullBoost getNearestBoost(List<FullBoost> boosts, Vector3 position) {
        FullBoost location = null;
        double minDistance = Double.MAX_VALUE;
        for (FullBoost boost : boosts) {
            if (boost.isActive) {
                double distance = position.distance(boost.location);
                if (distance < minDistance) {
                    minDistance = distance;
                    location = boost;
                }
            }
        }
        return location;
    }

    public static boolean seesOpportunisticBoost(CarData carData, List<FullBoost> boosts) {
        FullBoost boost = getNearestBoost(boosts, carData.position);
        return boost.location.distance(carData.position) < 20 &&
                Math.abs(SteerUtil.getCorrectionAngleRad(carData, boost.location)) < Math.PI / 6;

    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return "Going for boost";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
