package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.travel.SlideToPositionStep;
import tarehart.rlbot.tuning.BotLog;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class GetOnDefenseStep implements Step {
    private static final double CENTER_OFFSET = Goal.EXTENT * .5;
    private static final double AWAY_FROM_GOAL = 3;
    private static final double DEFAULT_LIFESPAN = 1;
    private double lifespan; // seconds
    private Plan plan;
    private LocalDateTime startTime;

    public GetOnDefenseStep() {
        this(DEFAULT_LIFESPAN);
    }

    public GetOnDefenseStep(double lifespan) {
        this.lifespan = lifespan;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (startTime == null) {
            startTime = input.time;
        }

        if (TimeUtil.secondsBetween(startTime, input.time) > lifespan && (plan == null || plan.canInterrupt())) {
            return Optional.empty(); // Time to reevaluate the plan.
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        plan = new Plan(Plan.Posture.DEFENSIVE).withStep(new SlideToPositionStep(in -> {

            Vector3 goalCenter = GoalUtil.getOwnGoal(in.team).getCenter();
            Vector3 futureBallPosition = TacticsTelemetry.get(in.playerIndex)
                    .map(telem -> telem.futureBallMotion.space)
                    .orElse(in.ballPosition);

            Vector2 targetPosition = new Vector2(Math.signum(futureBallPosition.x) * CENTER_OFFSET, goalCenter.y - Math.signum(goalCenter.y) * AWAY_FROM_GOAL);
            Vector2 targetFacing = new Vector2(-Math.signum(targetPosition.x), 0);
            return new PositionFacing(targetPosition, targetFacing);
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
