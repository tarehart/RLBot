package tarehart.rlbot.steps.wall;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.util.Optional;

public class MountWallStep implements Step {
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (ArenaModel.isCarOnWall(car)) {
            // Successfully made it onto the wall
            return Optional.empty();
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);
        if (!WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
            // Failed to mount the wall in time.
            return Optional.empty();
        }

        BallSlice ballMotion = ballPath.getMotionAt(input.time.plusSeconds(3)).orElse(ballPath.getEndpoint());
        Vector3 ballPositionExaggerated = ballMotion.getSpace().scaled(1.04); // This assumes the ball is close to the wall

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, ballPositionExaggerated));
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public String getSituation() {
        return "Mounting the wall";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
