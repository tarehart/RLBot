package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.routing.PositionFacing;
import tarehart.rlbot.planning.TacticsTelemetry;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.travel.SlideToPositionStep;

import java.awt.*;
import java.util.Optional;

public class RotateAndWaitToClearStep implements Step {
    private static final double CENTER_OFFSET = -2;
    private static final double AWAY_FROM_GOAL = -2;
    private Plan plan;

    public RotateAndWaitToClearStep() {
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {
        CarData myCar = input.getMyCarData();

        if (TacticsTelemetry.get(myCar.playerIndex).map(telem -> !telem.waitToClear).orElse(false)) {
            return Optional.empty(); // Time to reevaluate the plan.
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        plan = new Plan(Plan.Posture.DEFENSIVE)
                .withStep(new SlideToPositionStep(in -> {

                    Vector3 goalCenter = GoalUtil.getOwnGoal(in.team).getCenter();
                    Vector3 futureBallPosition = TacticsTelemetry.get(in.playerIndex)
                            .map(telem -> telem.futureBallMotion.space)
                            .orElse(in.ballPosition);

                    Vector2 targetPosition = new Vector2(Math.signum(futureBallPosition.x) * CENTER_OFFSET, goalCenter.y - Math.signum(goalCenter.y) * AWAY_FROM_GOAL);
                    Vector2 targetFacing = new Vector2(-Math.signum(targetPosition.x), 0);
                    return new PositionFacing(targetPosition, targetFacing);
                }))
                .withStep(new BlindStep(1, new AgentOutput()));

        return plan.getOutput(input);
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return "Rotating and waiting to clear";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}


