package tarehart.rlbot;

import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.ui.Readout;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

import static tarehart.rlbot.tuning.BotLog.println;

public abstract class Bot {

    private final Team team;
    private final int playerIndex;
    Plan currentPlan = null;
    ZonePlan currentZonePlan = null;
    private Readout readout;
    private String previousSituation = null;
    ZonePlan previousZonePlan = null;

    public enum Team {
        BLUE,
        ORANGE
    }

    public Bot(Team team, int playerIndex) {
        this.team = team;
        this.playerIndex = playerIndex;
        readout = new Readout();
    }


    public AgentOutput processInput(AgentInput input) {
        BotLog.setTimeStamp(input.matchInfo.timeRemaining.getSeconds());
        final AgentOutput output;
        Optional<BallPath> ballPath = Optional.empty();

        if (input.matchInfo.matchEnded) {
            currentPlan = new Plan(Plan.Posture.MENU);
            output = new AgentOutput();
        } else {
            ballPath = Optional.of(ArenaModel.predictBallPath(input));
            ZonePlan zonePlan = new ZonePlan(input);
            ZoneTelemetry.set(zonePlan, input.team);

//        BallRecorder.recordPosition(new BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin));
//        if (input.ballVelocity.magnitudeSquared() > 0) {
//            Optional<BallSlice> afterBounce = ballPath.getMotionAfterWallBounce(1);
//            // Just for data gathering / debugging.
//            afterBounce.ifPresent(stv -> BallRecorder.startRecording(
//                    new BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin),
//                    stv.getTime().plusSeconds(1)));
//        }

            if (input.matchInfo.roundActive) {
                output = getOutput(input);
            } else {
                output = new AgentOutput();
            }
        }

        Plan.Posture posture = currentPlan != null ? currentPlan.getPosture() : Plan.Posture.NEUTRAL;
        String situation = currentPlan != null ? currentPlan.getSituation() : "";
        if (!Objects.equals(situation, previousSituation)) {
            println("[Sitch] " + situation, input.playerIndex);
        }
        previousSituation = situation;
        ballPath.ifPresent(bp -> readout.update(input, posture, situation, BotLog.collect(input.playerIndex), bp));

        return output;
    }

    protected abstract AgentOutput getOutput(AgentInput input);

    protected boolean canInterruptPlanFor(Plan.Posture posture) {
        return currentPlan == null || currentPlan.isComplete() || currentPlan.getPosture().lessUrgentThan(posture) && currentPlan.canInterrupt();
    }

    protected boolean noActivePlanWithPosture(Plan.Posture posture) {
        return currentPlan == null || currentPlan.isComplete() || currentPlan.getPosture() != posture;
    }

    public JFrame getDebugWindow() {
        JFrame frame = new JFrame("Debug - " + team.name());
        frame.setContentPane(readout.getRootPanel());
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
        return frame;
    }
}
