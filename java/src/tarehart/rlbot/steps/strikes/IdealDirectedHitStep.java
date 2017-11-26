package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.util.Optional;

public class IdealDirectedHitStep implements Step {
    private Step proxyStep;

    public IdealDirectedHitStep(KickStrategy kickStrategy, AgentInput input) {
        DirectedNoseHitStep noseHit = new DirectedNoseHitStep(kickStrategy);

        if (noseHit.getOutput(input).isPresent() && Math.abs(noseHit.getEstimatedAngleOfKickFromApproach()) < Math.PI / 2) {
            proxyStep = noseHit;
        } else {
            proxyStep = new DirectedSideHitStep(kickStrategy);
        }
    }

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {
        return proxyStep.getOutput(input);
    }

    @Override
    public String getSituation() {
        return proxyStep.getSituation();
    }

    @Override
    public boolean canInterrupt() {
        return proxyStep.canInterrupt();
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
