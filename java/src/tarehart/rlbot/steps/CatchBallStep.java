package tarehart.rlbot.steps;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.AirTouchPlanner;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.SteerUtil;

import java.util.Optional;

public class CatchBallStep implements Step {

    private int confusionLevel = 0;
    private SpaceTime latestCatchLocation;
    private boolean firstFrame = true;

    public CatchBallStep(SpaceTime initialCatchLocation) {
        latestCatchLocation = initialCatchLocation;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (firstFrame) {
            firstFrame = false;
            return Optional.of(playCatch(car, latestCatchLocation));
        }

        double distance = car.position.distance(input.ballPosition);

        if (distance < 2.5 || confusionLevel > 3) {
            return Optional.empty();
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);
        Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(car, ballPath, AirTouchPlanner.getBoostBudget(car));

        // Weed out any intercepts after a catch opportunity. Should just catch it.
        if (catchOpportunity.isPresent()) {
            latestCatchLocation = catchOpportunity.get();
            confusionLevel = 0;
        } else {
            confusionLevel++;
        }

        return Optional.of(playCatch(car, latestCatchLocation));
    }

    private AgentOutput playCatch(CarData car, SpaceTime catchLocation) {
        Vector3 enemyGoal = GoalUtil.getEnemyGoal(car.team).getCenter();
        Vector3 awayFromEnemyGoal = catchLocation.space.minus(enemyGoal);
        Vector3 offset = new Vector3(awayFromEnemyGoal.x, awayFromEnemyGoal.y, 0).scaledToMagnitude(1.2);
        Vector3 target = catchLocation.space.plus(offset);

        return SteerUtil.getThereOnTime(car, new SpaceTime(target, catchLocation.time));
    }

    @Override
    public String getSituation() {
        return "Catching ball";
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }
}
