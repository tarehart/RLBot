package tarehart.rlbot.steps.debug;

import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerPlan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class TagAlongStep implements Step {

    private Plan plan;


    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        CarData car = input.getMyCarData();
        Optional<CarData> enemyCarOption = input.getEnemyCarData();
        if (!enemyCarOption.isPresent()) {
            return Optional.empty();
        }

        CarData enemyCar = enemyCarOption.get();

        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost, 0);

        Vector2 waypoint = enemyCar.position.plus(enemyCar.orientation.rightVector.scaled(4)).flatten();
        Vector2 targetFacing = enemyCar.orientation.noseVector.flatten();
        SteerPlan steerPlan = SteerUtil.getPlanForCircleTurn(car, fullAcceleration, waypoint, targetFacing);

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, steerPlan.waypoint);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip toward tag along", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        return Optional.of(steerPlan.immediateSteer);
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
        return false;
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Tagging along", plan);
    }
}
