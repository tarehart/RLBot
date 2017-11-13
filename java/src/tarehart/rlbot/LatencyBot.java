package tarehart.rlbot;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.planning.SteerUtil;

public class LatencyBot extends Bot {

    public LatencyBot(Team team, int playerIndex) {
        super(team, playerIndex);
    }

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        if (VectorUtil.flatDistance(input.ballPosition, new Vector3()) > 0) {
            return new AgentOutput().withJump();
        }

        final CarData car = input.getMyCarData();
        return SteerUtil.steerTowardGroundPosition(car, input.ballPosition.plus(new Vector3(20, 0, 0)));
    }
}
