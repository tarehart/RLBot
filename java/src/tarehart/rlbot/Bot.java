package tarehart.rlbot;

import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.tuning.BallRecorder;
import tarehart.rlbot.tuning.BallTelemetry;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.ui.Readout;

import javax.swing.*;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public abstract class Bot {

    private final Team team;
    Plan currentPlan = null;
    ZonePlan currentZonePlan = null;
    private Readout readout;
    private String previousSituation = null;
    ZonePlan previousZonePlan = null;

    public enum Team {
        BLUE,
        ORANGE
    }

    public Bot(Team team) {
        this.team = team;
        readout = new Readout();
    }


    public AgentOutput processInput(AgentInput input) {

        BallPath ballPath = ArenaModel.predictBallPath(input, 5);
        BallTelemetry.setPath(ballPath, input.team);
        ZonePlan zonePlan = new ZonePlan(input);
        ZoneTelemetry.set(zonePlan, input.team);

//        BallRecorder.recordPosition(new BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin));
//        Optional<BallSlice> afterBounce = ballPath.getMotionAfterWallBounce(1);
//        // Just for data gathering / debugging.
//        afterBounce.ifPresent(stv -> BallRecorder.startRecording(
//                new BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin),
//                stv.getTime().plusSeconds(1)));


        AgentOutput output = getOutput(input);

        Plan.Posture posture = currentPlan != null ? currentPlan.getPosture() : Plan.Posture.NEUTRAL;
        String situation = currentPlan != null ? currentPlan.getSituation() : "";
        if (!Objects.equals(situation, previousSituation)) {
            BotLog.println("[Sitch] " + situation, input.team);
        }
        previousSituation = situation;
        readout.update(input, posture, situation, BotLog.collect(input.team), BallTelemetry.getPath(input.team).get());
        BallTelemetry.reset(input.team);

        return output;
    }

    protected abstract AgentOutput getOutput(AgentInput input);

    protected boolean canInterruptPlanFor(Plan.Posture posture) {
        return currentPlan == null || currentPlan.getPosture().lessUrgentThan(posture) && currentPlan.canInterrupt();
    }

    protected boolean noActivePlanWithPosture(Plan.Posture posture) {
        return currentPlan == null || currentPlan.getPosture() != posture;
    }

    public JFrame getDebugWindow() {
        JFrame frame = new JFrame("Debug - " + team.name());
        frame.setContentPane(readout.getRootPanel());
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
        return frame;
    }
}
