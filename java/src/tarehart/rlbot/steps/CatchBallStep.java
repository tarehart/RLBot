package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.intercept.AirTouchPlanner;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.SteerUtil;

import java.awt.*;
import java.util.Optional;

public class CatchBallStep implements Step {
    private SpaceTime latestCatchLocation;

    public CatchBallStep() {
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        BallPath ballPath = ArenaModel.predictBallPath(input);
        Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(car, ballPath, AirTouchPlanner.getBoostBudget(car));

        // Weed out any intercepts after a catch opportunity. Should just catch it.
        if (catchOpportunity.isPresent()) {
            latestCatchLocation = catchOpportunity.get();
            return Optional.of(playCatch(car, latestCatchLocation));
        } else {
            return Optional.empty();
        }
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

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
