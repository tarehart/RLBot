package tarehart.rlbot;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.GoForKickoffStep;
import tarehart.rlbot.steps.demolition.DemolishEnemyStep;
import tarehart.rlbot.steps.landing.LandGracefullyStep;
import tarehart.rlbot.time.GameTime;

import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class ReliefBot extends Bot {

    private TacticsAdvisor tacticsAdvisor;

    public ReliefBot(Team team, int playerIndex) {
        super(team, playerIndex);
        tacticsAdvisor = new TacticsAdvisor();
    }

    private GameTime startTime;

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        final CarData car = input.getMyCarData();

//
//        double enemyHeight = input.getEnemyCarData().get().position.z;
//        if (enemyHeight > .3407) {
//            if (startTime == null) {
//                startTime = input.time;
//            }
//            BotLog.println(String.format("%.2f %.2f", TimeUtil.toSeconds(Duration.between(startTime, input.time)), enemyHeight), car.playerIndex);
//        } else {
//            if (startTime != null) {
//                BotLog.println(Duration.between(startTime, input.time).toMillis() + "", car.playerIndex);
//                startTime = null;
//            }
//        }
//
//        return new AgentOutput();

        Optional<ZonePlan> zonePlan = ZoneTelemetry.get(input.getTeam());
        BallPath ballPath = ArenaModel.predictBallPath(input);
        TacticalSituation situation = tacticsAdvisor.assessSituation(input, ballPath, currentPlan);

//        if (canInterruptPlanFor(Plan.Posture.OVERRIDE)) {
//            currentPlan = new Plan(Plan.Posture.OVERRIDE).withStep(new GetBoostStep()).withStep(new DemolishEnemyStep());
//        }

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (noActivePlanWithPosture(Plan.Posture.KICKOFF) && (!zonePlan.isPresent() || situation.getGoForKickoff())) {
            currentPlan = new Plan(Plan.Posture.KICKOFF).withStep(new GoForKickoffStep());
        }

        if (canInterruptPlanFor(Plan.Posture.LANDING) && !car.getHasWheelContact() &&
                car.getPosition().getZ() > 5 &&
                !ArenaModel.isBehindGoalLine(car.getPosition())) {
            currentPlan = new Plan(Plan.Posture.LANDING).withStep(new LandGracefullyStep(LandGracefullyStep.FACE_BALL));
        }

        if (situation.getScoredOnThreat() != null && canInterruptPlanFor(Plan.Posture.SAVE)) {
            println("Canceling current plan. Need to go for save!", input.getPlayerIndex());
            currentPlan = null;
        } else if (zonePlan.isPresent() && situation.getForceDefensivePosture() && canInterruptPlanFor(Plan.Posture.DEFENSIVE)) {
            println("Canceling current plan. Forcing defensive rotation!", input.getPlayerIndex());
            currentPlan = null;
        } else if (situation.getWaitToClear() && canInterruptPlanFor(Plan.Posture.WAITTOCLEAR)) {
            println("Canceling current plan. Ball is in the corner and I need to rotate!", input.getPlayerIndex());
            currentPlan = null;
        } else if (situation.getNeedsDefensiveClear() && canInterruptPlanFor(Plan.Posture.CLEAR)) {
            println("Canceling current plan. Going for clear!", input.getPlayerIndex());
            currentPlan = null;
        } else if (situation.getShotOnGoalAvailable() && canInterruptPlanFor(Plan.Posture.OFFENSIVE)) {
            println("Canceling current plan. Shot opportunity!", input.getPlayerIndex());
            currentPlan = null;
        }

        if (currentPlan == null || currentPlan.isComplete()) {
            currentPlan = tacticsAdvisor.makePlan(input, situation);
        }

        if (currentPlan != null) {
            if (currentPlan.isComplete()) { //REVIEW: Possibly redundant check considering that .makePlan() is called if it is complete
                currentPlan = null;
            } else {
                Optional<AgentOutput> output = currentPlan.getOutput(input);
                if (output.isPresent()) {
                    return output.get();
                }
            }
        }

        return SteerUtil.INSTANCE.steerTowardGroundPosition(car, input.getBoostData(), input.getBallPosition().flatten());
    }
}
