package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.planning.TacticalSituation;
import tarehart.rlbot.planning.TacticsTelemetry;

import java.awt.*;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class ChaseBallStep implements Step {
    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        Optional<TacticalSituation> tacticalSituationOption = TacticsTelemetry.get(input.getPlayerIndex());

        if (tacticalSituationOption.map(situation -> situation.expectedContact.isPresent()).orElse(false)) {
            // There's an intercept, quit this thing.
            // TODO: sometimes the intercept step fails to pick up on this because its accelration model does no front flips.
            return Optional.empty();
        }

        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, input.getBallPosition());
        if (sensibleFlip.isPresent()) {
            println("Front flip after ball", input.getPlayerIndex());
            plan = sensibleFlip.get();
            return plan.getOutput(input);
        }

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.getBallPosition()));
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return Plan.Companion.concatSituation("Chasing ball", plan);
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
