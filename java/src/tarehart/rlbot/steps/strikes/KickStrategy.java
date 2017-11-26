package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.vector.Vector3;

public interface KickStrategy {
    Vector3 getKickDirection(AgentInput input);
    Vector3 getKickDirection(AgentInput input, Vector3 ballPosition);
    Vector3 getKickDirection(AgentInput input, Vector3 ballPosition, Vector3 easyKick);
}
