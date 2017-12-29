package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.util.Optional;

public class BlindStep implements Step {
    private AgentOutput output;
    private Duration duration;
    private GameTime scheduledEndTime;


    public BlindStep(double seconds, AgentOutput output) {
        this.output = output;
        this.duration = Duration.ofSeconds(seconds);
    }

    public BlindStep(AgentOutput output, Duration duration) {
        this.output = output;
        this.duration = duration;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {
        if (scheduledEndTime == null) {
            scheduledEndTime = input.getTime().plus(duration);
        }

        if (input.getTime().isAfter(scheduledEndTime)) {
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
