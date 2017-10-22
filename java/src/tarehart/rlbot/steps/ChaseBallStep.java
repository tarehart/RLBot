package tarehart.rlbot.steps;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.AirTouchPlanner;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.strikes.InterceptStep;

import java.time.Duration;
import java.util.Optional;

public class ChaseBallStep implements Step {

    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        CarData car = input.getMyCarData();

        if (car.position.z > 1 && !ArenaModel.isCarNearWall(car)) {
            return Optional.empty();
        }


        BallPath ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(3));

        if (input.getEnemyCarData().map(c -> c.position.distance(input.ballPosition)).orElse(Double.MAX_VALUE) > 50) {
            if (car.boost < 10 && GetBoostStep.seesOpportunisticBoost(car, input.fullBoosts)) {
                plan = new Plan().withStep(new GetBoostStep());
                plan.begin();
                return plan.getOutput(input);
            }

            Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(car, ballPath, AirTouchPlanner.getBoostBudget(car));
            if (catchOpportunity.isPresent()) {
                plan = new Plan().withStep(new CatchBallStep(catchOpportunity.get())).withStep(new DribbleStep());
                plan.begin();
                return plan.getOutput(input);
            }
        }

        InterceptStep interceptStep = new InterceptStep(new Vector3());
        Optional<AgentOutput> output = interceptStep.getOutput(input);
        if (output.isPresent()) {
            plan = new Plan().withStep(interceptStep);
            plan.begin();
            return output;
        }

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.ballPosition));
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
    public String getSituation() {
        return Plan.concatSituation("Chasing ball", plan);
    }
}
