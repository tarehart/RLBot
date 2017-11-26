package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.Step;

import java.util.ArrayList;
import java.util.Optional;

public class Plan {

    private final Posture posture;
    private boolean unstoppable;
    private ArrayList<Step> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private boolean isComplete = false;

    public static String concatSituation(String baseSituation, Plan plan) {
        return baseSituation + (plan != null && !plan.isComplete() ? "(" + plan.getSituation() + ")" : "");
    }

    public boolean canInterrupt() {
        return isComplete() || !unstoppable && getCurrentStep().canInterrupt();
    }

    public enum Posture {
        NEUTRAL(0),
        OFFENSIVE(1),
        DEFENSIVE(5),
        CLEAR(8),
        WAITTOCLEAR(9),
        SAVE(10),
        LANDING(15),
        KICKOFF(50),
        MENU(75),
        OVERRIDE(100);

        private int urgency;

        Posture(int urgency) {
            this.urgency = urgency;
        }

        public boolean lessUrgentThan(Posture other) {
            return urgency < other.urgency;
        }
    }

    public Plan() {
        this(Posture.NEUTRAL);
    }

    public Plan(Posture posture) {
        this.posture = posture;
    }

    public Plan withStep(Step step) {
        steps.add(step);
        return this;
    }

    public Plan unstoppable() {
        this.unstoppable = true;
        return this;
    }

    public Plan appendPlan(Plan plan) {
        steps.addAll(plan.steps);
        return this;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (isComplete) {
            throw new RuntimeException("Plan is already complete!");
        }

        while (currentStepIndex < steps.size()) {
            Step currentStep = getCurrentStep();

            Optional<AgentOutput> output = currentStep.getOutput(input);
            if (output.isPresent()) {
                return output;
            }

            nextStep();
        }

        isComplete = true;
        return Optional.empty();
    }

    /**
     * You should make sure the plan is not complete before calling this.
     */
    public Step getCurrentStep() {
        return steps.get(currentStepIndex);
    }

    private void nextStep() {
        currentStepIndex++;
    }

    public String getSituation() {
        if (isComplete()) {
            return "Dead plan";
        }
        return posture.name() + " " + (currentStepIndex + 1) + ". " + getCurrentStep().getSituation();
    }

    public boolean isComplete() {
        return isComplete || currentStepIndex >= steps.size();
    }

    public Posture getPosture() {
        return posture;
    }
}
