package tarehart.rlbot.steps.landing;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.util.Optional;

public class LandMindlesslyStep implements Step {
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (car.getPosition().getZ() < .40f || ArenaModel.isCarNearWall(car) && car.getPosition().getZ() < 5) {
            return Optional.empty();
        }

        if (ArenaModel.isCarOnWall(car)) {
            Vector3 groundBeneathMe = new Vector3(car.getPosition().getX(), car.getPosition().getY(), 0);
            return Optional.of(SteerUtil.steerTowardWallPosition(car, groundBeneathMe));
        }

        return Optional.of(new AgentOutput().withAcceleration(1));
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Waiting to land";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
