package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class BlindStep implements Step {
    private AgentOutput output;
    private Duration duration;
    private LocalDateTime scheduledEndTime;

    public BlindStep(AgentOutput output, Duration duration) {
        this.output = output;
        this.duration = duration;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {
        if (scheduledEndTime == null) {
            scheduledEndTime = input.time.plus(duration);
        }

        if (input.time.isAfter(scheduledEndTime)) {
            return Optional.empty();
        }
        return Optional.of(output);
    }

    @Override
    public String getSituation() {
        return "Muscle memory";
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
