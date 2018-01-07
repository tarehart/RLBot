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
import tarehart.rlbot.time.Duration;

import java.awt.*;
import java.util.Optional;

public class CatchBallStep implements Step {
    private SpaceTime latestCatchLocation;

    public CatchBallStep() {
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        BallPath ballPath = ArenaModel.predictBallPath(input);
        Optional<SpaceTime> catchOpportunity = SteerUtil.INSTANCE.getCatchOpportunity(car, ballPath, AirTouchPlanner.INSTANCE.getBoostBudget(car));

        // Weed out any intercepts after a catch opportunity. Should just catch it.
        if (catchOpportunity.isPresent()) {
            latestCatchLocation = catchOpportunity.get();
            if (Duration.Companion.between(input.getTime(), latestCatchLocation.getTime()).getSeconds() > 2) {
                return Optional.empty(); // Don't wait around for so long
            }
            return Optional.of(playCatch(car, latestCatchLocation));
        } else {
            return Optional.empty();
        }
    }

    private AgentOutput playCatch(CarData car, SpaceTime catchLocation) {
        Vector3 enemyGoal = GoalUtil.INSTANCE.getEnemyGoal(car.getTeam()).getCenter();
        Vector3 awayFromEnemyGoal = catchLocation.getSpace().minus(enemyGoal);
        Vector3 offset = new Vector3(awayFromEnemyGoal.getX(), awayFromEnemyGoal.getY(), 0).scaledToMagnitude(1.2);
        Vector3 target = catchLocation.getSpace().plus(offset);

        return SteerUtil.INSTANCE.getThereOnTime(car, new SpaceTime(target, catchLocation.getTime()));
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
