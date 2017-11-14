package tarehart.rlbot.steps.defense;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class RotateAndWaitToClearStep implements Step {
    private static final double NEEDS_DEFENSE_THRESHOLD = 10;
    private static final double CENTER_OFFSET = Goal.EXTENT * .5;
    private static final double AWAY_FROM_GOAL = 0;
    private static final double LIFESPAN = 3;
    private Plan plan;
    private LocalDateTime startTime;

    public RotateAndWaitToClearStep() {}

    public Optional<AgentOutput> getOutput(AgentInput input) {
        TacticalSituation tacticalSituation = null;
        if(TacticsTelemetry.get(input.team).isPresent())
            tacticalSituation = TacticsTelemetry.get(input.team).get();

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (startTime == null) {
            startTime = input.time;
        }

        if ((tacticalSituation != null && tacticalSituation.waitToClear)
                || TimeUtil.secondsBetween(startTime, input.time) > LIFESPAN) {
            return Optional.empty(); // Time to reevaluate the plan.
        }

        CarData car = input.getMyCarData();

        Vector3 goalCenter = GoalUtil.getOwnGoal(input.team).getCenter();
        Vector2 targetPosition = new Vector2(Math.signum(input.ballPosition.x) * CENTER_OFFSET, goalCenter.y - Math.signum(goalCenter.y) * AWAY_FROM_GOAL);
        Vector2 targetFacing = new Vector2(-Math.signum(targetPosition.x), 0);

        double distance = car.position.flatten().distance(targetPosition);
        DistancePlot distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5), car.boost - 20, distance);

        SteerPlan planForCircleTurn = SteerUtil.getPlanForCircleTurn(car, distancePlot, targetPosition, targetFacing);

        //TODO: Make sure that this flip is finished even if the reevaluation time is hit and the plan/posture changes
        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, planForCircleTurn.waypoint);
        if (sensibleFlip.isPresent()) {
            println("Front flip for defense", input.playerIndex);
            plan = sensibleFlip.get();
            return plan.getOutput(input);
        } else {
            return Optional.of(planForCircleTurn.immediateSteer);
        }
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() { return "Rotating and waiting to clear"; }
}


