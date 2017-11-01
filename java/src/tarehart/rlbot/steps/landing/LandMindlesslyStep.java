package tarehart.rlbot.steps.landing;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;

import java.util.Optional;

public class LandMindlesslyStep implements Step {

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (car.position.z < .40f || ArenaModel.isCarNearWall(car) && car.position.z < 5) {
            return Optional.empty();
        }

        if (ArenaModel.isCarOnWall(car)) {
            Vector3 groundBeneathMe = new Vector3(car.position.x, car.position.y, 0);
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
}
