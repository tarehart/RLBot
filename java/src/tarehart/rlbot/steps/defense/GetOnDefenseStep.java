package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.routing.PositionFacing;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.travel.SlideToPositionStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.util.Optional;

public class GetOnDefenseStep implements Step {
    private static final double CENTER_OFFSET = Goal.EXTENT * .5;
    private static final double AWAY_FROM_GOAL = 3;
    private static final double DEFAULT_LIFESPAN = 1;
    private double lifespan; // seconds
    private Plan plan;
    private GameTime startTime;

    public GetOnDefenseStep() {
        this(DEFAULT_LIFESPAN);
    }

    public GetOnDefenseStep(double lifespan) {
        this.lifespan = lifespan;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (startTime == null) {
            startTime = input.getTime();
        }

        if (Duration.Companion.between(startTime, input.getTime()).getSeconds() > lifespan && (plan == null || plan.canInterrupt())) {
            return Optional.empty(); // Time to reevaluate the plan.
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        plan = new Plan(Plan.Posture.DEFENSIVE).withStep(new SlideToPositionStep(in -> {

            Vector3 goalCenter = GoalUtil.INSTANCE.getOwnGoal(in.getTeam()).getCenter();
            Vector3 futureBallPosition = TacticsTelemetry.get(in.getPlayerIndex())
                    .map(telem -> telem.futureBallMotion.getSpace())
                    .orElse(in.getBallPosition());

            Vector2 targetPosition = new Vector2(Math.signum(futureBallPosition.getX()) * CENTER_OFFSET, goalCenter.getY() - Math.signum(goalCenter.getY()) * AWAY_FROM_GOAL);
            Vector2 targetFacing = new Vector2(-Math.signum(targetPosition.getX()), 0);
            return Optional.of(new PositionFacing(targetPosition, targetFacing));
        }));

        return plan.getOutput(input);
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return "Getting on defense";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
