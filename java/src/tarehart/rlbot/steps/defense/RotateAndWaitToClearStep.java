package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class RotateAndWaitToClearStep implements Step {
    private static final double NEEDS_DEFENSE_THRESHOLD = 10;
    private static final double CENTER_OFFSET = Goal.EXTENT * .5;
    private static final double AWAY_FROM_GOAL = 3;
    private static final double LIFESPAN = 0.1; // seconds
    private Plan plan;
    private LocalDateTime startTime;

    public RotateAndWaitToClearStep() {}

    public Optional<AgentOutput> getOutput(AgentInput input) {
        return null;
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() { return "Rotating and waiting to clear"; }
}


