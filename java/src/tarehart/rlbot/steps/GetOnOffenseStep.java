package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.intercept.Intercept;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.routing.PositionFacing;
import tarehart.rlbot.steps.travel.SlideToPositionStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class GetOnOffenseStep implements Step {
    private Plan plan;

    private Duration duration;
    private GameTime lastMoment;
    private Vector3 originalTarget;
    private Vector3 target;

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

//        if (input.getTime().isAfter(lastMoment) && (plan == null || plan.canInterrupt())) {
//            return Optional.empty();
//        }



        Optional<TacticalSituation> tacticalSituationOption = TacticsTelemetry.get(input.getPlayerIndex());

        BallPath ballPath = ArenaModel.predictBallPath(input);

        SpaceTime ballFuture = tacticalSituationOption.map(situation -> situation.expectedContact.map(Intercept::toSpaceTime))
                .orElse(ballPath.getMotionAt(input.getTime().plusSeconds(4)).map(BallSlice::toSpaceTime))
                .orElse(new SpaceTime(input.getBallPosition(), input.getTime()));

        CarData car = input.getMyCarData();

        if (car.getBoost() < 10 && GetBoostStep.seesOpportunisticBoost(car, input.getBoostData().getFullBoosts())) {
            plan = new Plan().withStep(new GetBoostStep());
            return plan.getOutput(input);
        }

        Goal enemyGoal = GoalUtil.getEnemyGoal(input.getTeam());
        Goal ownGoal = GoalUtil.getOwnGoal(input.getTeam());

        target = ballFuture.getSpace();

        double backoff = 15 + ballFuture.getSpace().getZ();
        Vector2 facing;

        if (Math.abs(target.getX()) < ArenaModel.SIDE_WALL * .8) {
            // Get into a strike position, 10 units behind the ball
            Vector3 goalToBall = target.minus(enemyGoal.getNearestEntrance(target, -10));
            Vector3 goalToBallNormal = goalToBall.normaliseCopy();
            facing = goalToBallNormal.flatten().scaled(-1);
            target = target.plus(goalToBallNormal.scaled(backoff));

        } else {
            // Get into a backstop position
            Vector3 goalToBall = target.minus(ownGoal.getCenter());
            Vector3 goalToBallNormal = goalToBall.normaliseCopy();
            facing = goalToBallNormal.flatten().scaled(-1);
            target = target.minus(goalToBallNormal.scaled(backoff));
        }

        if (originalTarget == null) {
            originalTarget = target;
        }

        boolean canInterruptPlan = plan == null || plan.canInterrupt();

        if ((TacticsAdvisor.getYAxisWrongSidedness(car, ballFuture.getSpace()) < -backoff * .6 ||
                target.distance(originalTarget) > 10 ||
                !ArenaModel.isInBoundsBall(target)) && canInterruptPlan) {
            return Optional.empty();
        }

        if (plan != null && !plan.isComplete()) {
            Optional<AgentOutput> output = plan.getOutput(input);
            if (output.isPresent()) {
                return output;
            }
        }

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, target);
        if (sensibleFlip.isPresent()) {
            println("Front flip toward offense", input.getPlayerIndex());
            plan = sensibleFlip.get();
            return plan.getOutput(input);
        }

        plan = new Plan().withStep(new SlideToPositionStep(in -> Optional.of(new PositionFacing(target.flatten(), facing))));
        return plan.getOutput(input);
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
        if (Plan.Companion.activePlan(plan).isPresent()) {
            plan.getCurrentStep().drawDebugInfo(graphics);
        }

        if (target != null) {
            graphics.setColor(new Color(190, 61, 66));
            graphics.setStroke(new BasicStroke(1));
            Vector2 position = target.flatten();
            int crossSize = 2;
            graphics.draw(new Line2D.Double(position.getX() - crossSize, position.getY() - crossSize, position.getX() + crossSize, position.getY() + crossSize));
            graphics.draw(new Line2D.Double(position.getX() - crossSize, position.getY() + crossSize, position.getX() + crossSize, position.getY() - crossSize));
        }
    }
}
