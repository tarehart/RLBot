package tarehart.rlbot.steps.travel;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.util.Optional;

public class LineUpInReverseStep implements Step {

    private Vector2 waypoint;
    Integer correctionDirection = null;

    public LineUpInReverseStep(Vector2 waypoint) {
        this.waypoint = waypoint;
    }

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        Vector2 waypointToCar = car.position.flatten().minus(waypoint);

        double correctionRadians = car.orientation.noseVector.flatten().correctionAngle(waypointToCar);

        if (correctionDirection == null) {
            correctionDirection = (int) Math.signum(correctionRadians);
        }

        double futureRadians = correctionRadians + car.spin.yawRate * .3;

        if (futureRadians * correctionDirection < 0 && Math.abs(futureRadians) < Math.PI / 4) {
            return Optional.empty(); // Done orienting.
        }

        return Optional.of(new AgentOutput().withDeceleration(1).withSteer(correctionDirection));
    }

    @Override
    public String getSituation() {
        return "Lining up in reverse";
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
    }
}
