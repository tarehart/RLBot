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
    private static final double CENTER_OFFSET = -2;
    private static final double AWAY_FROM_GOAL = -2;
    private Plan plan;
    private LocalDateTime startTime;

    public RotateAndWaitToClearStep() {}

    public Optional<AgentOutput> getOutput(AgentInput input) {
        TacticalSituation tacticalSituation = null;
        if(TacticsTelemetry.get(input.team).isPresent())
            tacticalSituation = TacticsTelemetry.get(input.team).get();
        CarData myCar = input.getMyCarData();
        Vector3 myGoalCenter = GoalUtil.getOwnGoal(input.team).getCenter();


        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        if (startTime == null) {
            startTime = input.time;
        }

        if (tacticalSituation != null && !tacticalSituation.waitToClear) {
            return Optional.empty(); // Time to reevaluate the plan.
        }

        CarData car = input.getMyCarData();

        Vector3 goalCenter = GoalUtil.getOwnGoal(input.team).getCenter();
        Vector2 waypoint1 = new Vector2(
            Math.signum(input.ballPosition.x) * (CENTER_OFFSET - 4),
            goalCenter.y
        );
        Vector2 waypoint2 = new Vector2(
            Math.signum(input.ballPosition.x) * (CENTER_OFFSET - 2),
            goalCenter.y - (Math.signum(goalCenter.y) * (AWAY_FROM_GOAL - 3))
        );
        Vector2 waypoint3 = new Vector2(
            Math.signum(input.ballPosition.x) * CENTER_OFFSET,
            goalCenter.y - (Math.signum(goalCenter.y) * AWAY_FROM_GOAL)
        );

        Vector2 targetPosition;
        if(Math.abs(myCar.position.y) < Math.abs(waypoint1.y)) {
            targetPosition = waypoint1; // Outside of net, go for net entry vector
        }
        else if(Math.abs(myCar.position.y) > Math.abs(waypoint1.y) && Math.abs(myCar.position.y) < Math.abs(waypoint2.y))
        {
            targetPosition = waypoint2; // Entered net, start turning.
        }
        else
        {
            targetPosition = waypoint3; // Probably made it to waypoint 2, turn back towards net opening
        }

        // Check to see if we are in net and facing out
        boolean myCarIsInNet = Math.signum(myCar.position.y) == Math.signum(myGoalCenter.y)
                && Math.abs(myCar.position.y) > Math.abs(myGoalCenter.y);
        if (myCarIsInNet) {
            Vector2 leftPostVector = GoalUtil.getOwnGoal(input.team).getLeftPost().minus(myCar.position).flatten();
            Vector2 rightPostVector = GoalUtil.getOwnGoal(input.team).getRightPost().minus(myCar.position).flatten();
            double leftPostCorrection = myCar.orientation.noseVector.flatten().correctionAngle(leftPostVector);
            double rightPostCorrection = myCar.orientation.noseVector.flatten().correctionAngle(rightPostVector);

            boolean myCarIsFacingOut = Math.signum(leftPostCorrection) != Math.signum(rightPostCorrection)
                    && Math.abs(leftPostCorrection) < Math.toRadians(80)
                    && Math.abs(rightPostCorrection) < Math.toRadians(80);

            // If we are in net and facing out, just sit still
            if(myCarIsFacingOut) {
                return Optional.of(new AgentOutput());
            }
        }

        // If we aren't in net yet, go there
        AgentOutput planForStraightDrive = SteerUtil.steerTowardGroundPosition(car, targetPosition, true);
        return Optional.of(planForStraightDrive);
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() { return "Rotating and waiting to clear"; }
}


