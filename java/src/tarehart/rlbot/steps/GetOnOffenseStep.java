package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class GetOnOffenseStep implements Step {
    private Plan plan;

    private Duration duration;
    private GameTime lastMoment;


    public GetOnOffenseStep() {
        this(Duration.Companion.ofSeconds(1));
    }

    public GetOnOffenseStep(Duration duration) {
        this.duration = duration;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (lastMoment == null) {
            lastMoment = input.getTime().plus(duration);
        }

        if (input.getTime().isAfter(lastMoment) && (plan == null || plan.canInterrupt())) {
            return Optional.empty();
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        Optional<TacticalSituation> tacticalSituationOption = TacticsTelemetry.get(input.getPlayerIndex());

        if (tacticalSituationOption.map(situation -> situation.shotOnGoalAvailable).orElse(false)) {
            return Optional.empty();
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);

        SpaceTime ballFuture = tacticalSituationOption.map(situation -> situation.expectedContact.map(Intercept::toSpaceTime))
                .orElse(ballPath.getMotionAt(input.getTime().plusSeconds(2)).map(BallSlice::toSpaceTime))
                .orElse(new SpaceTime(input.getBallPosition(), input.getTime()));

        CarData car = input.getMyCarData();

        if (car.getBoost() < 10 && GetBoostStep.seesOpportunisticBoost(car, input.getFullBoosts())) {
            plan = new Plan().withStep(new GetBoostStep());
            return plan.getOutput(input);
        }

        Goal enemyGoal = GoalUtil.getEnemyGoal(input.getTeam());
        Goal ownGoal = GoalUtil.getOwnGoal(input.getTeam());

        Vector3 target = ballFuture.getSpace();



        if (Math.abs(target.getX()) < ArenaModel.SIDE_WALL * .8) {
            // Get into a strike position, 10 units behind the ball
            Vector3 goalToBall = target.minus(enemyGoal.getCenter());
            Vector3 goalToBallNormal = goalToBall.normaliseCopy();
            target = target.plus(goalToBallNormal.scaled(30));

        } else {
            // Get into a backstop position
            Vector3 goalToBall = target.minus(ownGoal.getCenter());
            Vector3 goalToBallNormal = goalToBall.normaliseCopy();
            target = target.minus(goalToBallNormal.scaled(30));
        }


        if (TacticsAdvisor.getYAxisWrongSidedness(car, ballFuture.getSpace()) < 0) {
            return Optional.empty();
        }

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, target);
        if (sensibleFlip.isPresent()) {
            println("Front flip toward offense", input.getPlayerIndex());
            plan = sensibleFlip.get();
            return plan.getOutput(input);
        }

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, target));
    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return "Getting on offense";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        if (Plan.activePlan(plan).isPresent()) {
            plan.getCurrentStep().drawDebugInfo(graphics);
        }
    }
}
