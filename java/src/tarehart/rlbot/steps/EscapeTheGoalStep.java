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
        if (!ArenaModel.isBehindGoalLine(car.getPosition())) {
            return Optional.empty();
        }

        Vector3 target = TacticsTelemetry.get(car.getPlayerIndex()).map(telem -> telem.futureBallMotion.getSpace()).orElse(new Vector3());
        Vector3 toTarget = target.minus(car.getPosition());
        Goal nearestGoal = GoalUtil.getNearestGoal(car.getPosition());
        Plane goalPlane = nearestGoal.getThreatPlane();
        Vector3 desiredExit = VectorUtil.getPlaneIntersection(goalPlane, car.getPosition(), toTarget).orElse(nearestGoal.getCenter());

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
