package tarehart.rlbot.steps;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.SteerUtil;

import java.util.Optional;

public class EscapeTheGoalStep implements Step {

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (!ArenaModel.isBehindGoalLine(car.position)) {
            return Optional.empty();
        }

        Vector3 target = new Vector3();
        return Optional.of(SteerUtil.steerTowardGroundPosition(car, target).withBoost(false));
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public String getSituation() {
        return "Escaping the goal";
    }
}
