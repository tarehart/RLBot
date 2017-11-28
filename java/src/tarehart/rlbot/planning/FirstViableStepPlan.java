package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.Step;

import java.util.Optional;

public class FirstViableStepPlan extends Plan {

    // If a step runs successfully for this number of frames, then we decide that it was viable, and commit to it.
    // When that step ends, the plan is complete, even if there were originally subsequent steps.
    private static final int FRAMES_TILL_COMMITMENT = 2;

    private int frameCount = 0;

    public FirstViableStepPlan(Posture posture) {
        super(posture);
    }

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (isComplete) {
            throw new RuntimeException("Plan is already complete!");
        }

        while (currentStepIndex < steps.size()) {
            Step currentStep = getCurrentStep();

            Optional<AgentOutput> output = currentStep.getOutput(input);
            if (output.isPresent()) {

                if (currentStepIndex < steps.size() - 1) {
                    frameCount += 1;
                    if (frameCount >= FRAMES_TILL_COMMITMENT) {
                        // Remove the subsequent steps.
                        for (int i = steps.size() - 1; i > currentStepIndex; i--) {
                            steps.remove(i);
                        }
                    }
                }

                return output;
            }

            nextStep();
        }

        isComplete = true;
        return Optional.empty();
    }

}
