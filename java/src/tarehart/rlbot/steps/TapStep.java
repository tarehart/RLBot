package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.tuning.BotLog;

import java.time.LocalDateTime;
import java.util.Optional;

public class TapStep implements Step {
    private AgentOutput output;
    private int numFrames;
    private int frameCount;
    private LocalDateTime previousTime;

    public TapStep(AgentOutput output) {
        this(1, output);
    }

    public TapStep(int numFrames, AgentOutput output) {
        this.output = output;
        this.numFrames = numFrames;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (previousTime == null || input.time.isAfter(previousTime)) {
            frameCount++;
            previousTime = input.time;
        }

        if (frameCount > numFrames) {
            return Optional.empty();
        }
        return Optional.of(output);
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Muscle memory";
    }
}
