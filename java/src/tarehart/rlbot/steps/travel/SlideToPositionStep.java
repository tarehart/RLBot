package tarehart.rlbot.steps.travel;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.planning.PositionFacing;
import tarehart.rlbot.steps.Step;

import java.awt.*;
import java.util.Optional;
import java.util.function.Function;

public class SlideToPositionStep implements Step {

    private Function<AgentInput, PositionFacing> targetFunction;

    public SlideToPositionStep(Function<AgentInput, PositionFacing> targetFunction) {
        this.targetFunction = targetFunction;
    }


    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {
        PositionFacing target = targetFunction.apply(input);

        CarData car = input.getMyCarData();



    }

    @Override
    public String getSituation() {
        return "Sliding to position";
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {

    }
}
