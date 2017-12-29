package tarehart.rlbot;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.debug.CalibrateStep;

public class LatencyBot extends Bot {

    private boolean hasJumped;

    public LatencyBot(Team team, int playerIndex) {
        super(team, playerIndex);
    }

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        if (!hasJumped) {
            if (VectorUtil.flatDistance(input.getBallPosition(), new Vector3()) > 0) {
                hasJumped = true;
                return new AgentOutput().withJump();
            }

            final CarData car = input.getMyCarData();
            return SteerUtil.steerTowardGroundPosition(car, input.getBallPosition().plus(new Vector3(20, 0, 0)));
        }

        if (noActivePlanWithPosture(Plan.Posture.NEUTRAL)) {
            currentPlan = new Plan().withStep(new CalibrateStep());
        }

        return currentPlan.getOutput(input).orElse(new AgentOutput());
    }
}
