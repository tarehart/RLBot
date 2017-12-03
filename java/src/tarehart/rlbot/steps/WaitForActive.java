package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;

import java.awt.*;
import java.util.Optional;

public class WaitForActive implements Step {

    public WaitForActive() {
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {
        if (input.matchInfo.roundActive) {
            return Optional.empty();
        }
        return Optional.of(new AgentOutput());
    }

    @Override
    public String getSituation() {
        return "Idling";
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
