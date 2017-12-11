package tarehart.rlbot;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.GoForKickoffStep;
import tarehart.rlbot.steps.debug.CalibrateStep;
import tarehart.rlbot.steps.debug.TagAlongStep;
import tarehart.rlbot.steps.landing.LandGracefullyStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public class ReliefBot extends Bot {

    private TacticsAdvisor tacticsAdvisor;

    public ReliefBot(Team team, int playerIndex) {
        super(team, playerIndex);
        tacticsAdvisor = new TacticsAdvisor();
    }

    private LocalDateTime startTime;

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

        Optional<ZonePlan> zonePlan = ZoneTelemetry.get(input.team);
        BallPath ballPath = ArenaModel.predictBallPath(input);
        TacticalSituation situation = tacticsAdvisor.assessSituation(input, ballPath, currentPlan);

//        if (canInterruptPlanFor(Plan.Posture.OVERRIDE)) {
//            currentPlan = new Plan(Plan.Posture.OVERRIDE).withStep(new TagAlongStep());
//        }

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (noActivePlanWithPosture(Plan.Posture.KICKOFF) && (!zonePlan.isPresent() || situation.goForKickoff)) {
            currentPlan = new Plan(Plan.Posture.KICKOFF).withStep(new GoForKickoffStep());
        }

        if (canInterruptPlanFor(Plan.Posture.LANDING) && !car.hasWheelContact &&
                car.position.z > 5 &&
                !ArenaModel.isBehindGoalLine(car.position)) {
            currentPlan = new Plan(Plan.Posture.LANDING).withStep(new LandGracefullyStep(LandGracefullyStep.FACE_BALL));
        }

        if (situation.scoredOnThreat.isPresent() && canInterruptPlanFor(Plan.Posture.SAVE)) {
            println("Canceling current plan. Need to go for save!", input.playerIndex);
            currentPlan = null;
        } else if (zonePlan.isPresent() && situation.forceDefensivePosture && canInterruptPlanFor(Plan.Posture.DEFENSIVE)) {
            println("Canceling current plan. Forcing defensive rotation!", input.playerIndex);
            currentPlan = null;
        } else if (situation.waitToClear && canInterruptPlanFor(Plan.Posture.WAITTOCLEAR)) {
            println("Canceling current plan. Ball is in the corner and I need to rotate!", input.playerIndex);
            currentPlan = null;
        } else if (situation.needsDefensiveClear && canInterruptPlanFor(Plan.Posture.CLEAR)) {
            println("Canceling current plan. Going for clear!", input.playerIndex);
            currentPlan = null;
        } else if (situation.shotOnGoalAvailable && canInterruptPlanFor(Plan.Posture.OFFENSIVE)) {
            println("Canceling current plan. Shot opportunity!", input.playerIndex);
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

        return SteerUtil.steerTowardGroundPosition(car, input.ballPosition);
    }
}
