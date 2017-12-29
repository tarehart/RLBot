package tarehart.rlbot.steps.wall;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.util.Optional;

public class DescendFromWallStep implements Step {
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (ArenaModel.isCarOnWall(car)) {
            Vector3 ballShadow = new Vector3(input.ballPosition.getX(), input.ballPosition.getY(), 0);
            return Optional.of(SteerUtil.steerTowardWallPosition(car, ballShadow));
        } else if (ArenaModel.isNearFloorEdge(car)) {
            return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.ballPosition));
        }

        return Optional.empty();
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Descending wall.";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
