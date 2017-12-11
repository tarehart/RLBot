package tarehart.rlbot.steps.debug;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.travel.SlideToPositionStep;

import java.awt.*;
import java.time.Duration;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class TagAlongStep implements Step {
    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        plan = new Plan().withStep(new SlideToPositionStep(in -> {
            Optional<CarData> enemyCarOption = in.getEnemyCarData();
            CarData enemyCar = enemyCarOption.get();

            Vector2 waypoint = enemyCar.position.plus(enemyCar.orientation.rightVector.scaled(4)).flatten();
            Vector2 targetFacing = enemyCar.orientation.noseVector.flatten();
            return new PositionFacing(waypoint, targetFacing);
        })).withStep(new BlindStep(new AgentOutput().withAcceleration(1), Duration.ofSeconds(3)));

        return plan.getOutput(input);

    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Tagging along", plan);
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        if (Plan.activePlan(plan).isPresent()) {
            plan.getCurrentStep().drawDebugInfo(graphics);
        }
    }
}
