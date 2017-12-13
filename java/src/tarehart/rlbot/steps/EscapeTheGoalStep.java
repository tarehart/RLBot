package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.Plane;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.planning.TacticsTelemetry;

import java.awt.*;
import java.util.Optional;

public class EscapeTheGoalStep implements Step {
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (!ArenaModel.isBehindGoalLine(car.position)) {
            return Optional.empty();
        }

        Vector3 target = TacticsTelemetry.get(car.playerIndex).map(telem -> telem.futureBallMotion.space).orElse(new Vector3());
        Vector3 toTarget = target.minus(car.position);
        Goal nearestGoal = GoalUtil.getNearestGoal(car.position);
        Plane goalPlane = nearestGoal.getThreatPlane();
        Vector3 desiredExit = VectorUtil.getPlaneIntersection(goalPlane, car.position, toTarget).orElse(nearestGoal.getCenter());

        Vector3 exit = nearestGoal.getNearestEntrance(desiredExit, 2);

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, exit).withBoost(false));
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public String getSituation() {
        return "Escaping the goal";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
