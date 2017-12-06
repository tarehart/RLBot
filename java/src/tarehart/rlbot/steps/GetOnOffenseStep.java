package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;

import java.awt.*;
import java.time.Duration;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class GetOnOffenseStep implements Step {
    private Plan plan;

    public static double getYAxisWrongSidedness(AgentInput input) {
        Vector3 ownGoalCenter = GoalUtil.getOwnGoal(input.team).getCenter();
        double playerToBallY = input.ballPosition.y - input.getMyCarData().position.y;
        return playerToBallY * Math.signum(ownGoalCenter.y);
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        Optional<TacticalSituation> tacticalSituationOption = TacticsTelemetry.get(input.playerIndex);

        if (tacticalSituationOption.map(situation -> situation.shotOnGoalAvailable).orElse(false)) {
            return Optional.empty();
        }

        BallPath ballPath = ArenaModel.predictBallPath(input);

        SpaceTime ballFuture = tacticalSituationOption.map(situation -> situation.expectedContact.map(Intercept::toSpaceTime))
                .orElse(ballPath.getMotionAt(input.time.plusSeconds(2)).map(BallSlice::toSpaceTime))
                .orElse(new SpaceTime(input.ballPosition, input.time));

        CarData car = input.getMyCarData();

        if (car.boost < 10 && GetBoostStep.seesOpportunisticBoost(car, input.fullBoosts)) {
            plan = new Plan().withStep(new GetBoostStep());
            return plan.getOutput(input);
        }

        Goal enemyGoal = GoalUtil.getEnemyGoal(input.team);
        Goal ownGoal = GoalUtil.getOwnGoal(input.team);

        Vector3 target = ballFuture.space;



        if (Math.abs(target.x) < ArenaModel.SIDE_WALL * .8) {
            // Get into a strike position, 10 units behind the ball
            Vector3 goalToBall = target.minus(enemyGoal.getCenter());
            Vector3 goalToBallNormal = goalToBall.normaliseCopy();
            target = target.plus(goalToBallNormal.scaled(10));

        } else {
            // Get into a backstop position
            Vector3 goalToBall = target.minus(ownGoal.getCenter());
            Vector3 goalToBallNormal = goalToBall.normaliseCopy();
            target = target.minus(goalToBallNormal.scaled(10));
        }

        if (car.position.distance(target) < 10) {
            return Optional.empty();
        }

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, target);
        if (sensibleFlip.isPresent()) {
            println("Front flip toward offense", input.playerIndex);
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
        // Draw nothing.
    }
}
