package tarehart.rlbot.ui;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.tuning.BallPrediction;
import tarehart.rlbot.tuning.PredictionWarehouse;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class Readout {

    private static final int HEIGHT_BAR_MULTIPLIER = 10;

    private JLabel planPosture;
    private JProgressBar ballHeightActual;
    private JProgressBar ballHeightPredicted;
    private JSlider predictionTime;
    private JPanel rootPanel;
    private BallPredictionRadar ballPredictionReadout;
    private JProgressBar ballHeightActualMax;
    private JProgressBar ballHeightPredictedMax;
    private JTextPane situationText;
    private JLabel predictionTimeSeconds;
    private JTextArea logViewer;
    private JLabel blueCarPosX;
    private JLabel blueCarPosY;
    private JLabel blueCarPosZ;
    private JLabel orangeCarPosX;
    private JLabel orangeCarPosY;
    private JLabel orangeCarPosZ;
    private JLabel ballPosX;
    private JLabel ballPosY;
    private JLabel ballPosZ;
    private JLabel ownGoalFutureProximity;
    private JLabel distanceBallIsBehindUs;
    private JLabel enemyOffensiveApproachError;
    private JLabel distanceFromEnemyBackWall;
    private JLabel distanceFromEnemyCorner;
    private JLabel needsDefensiveClear;
    private JLabel shotOnGoalAvailable;
    private JLabel expectedEnemyContact;
    private JLabel scoredOnThreat;
    private JLabel blueMainZone;
    private JLabel blueSubZone;
    private JLabel orangeMainZone;
    private JLabel orangeSubZone;
    private JLabel ballMainZone;
    private JLabel ballSubZone;
    private ArenaDisplay arenaDisplay;
    private JLabel goForKickoff;
    private JLabel waitToClear;
    private JLabel forceDefensivePosture;

    private double maxCarSpeedVal;

    private LocalDateTime actualMaxTime = LocalDateTime.now();
    private LocalDateTime predictedMaxTime = LocalDateTime.now();

    private PredictionWarehouse warehouse = new PredictionWarehouse();

    private LocalDateTime previousTime = null;


    public Readout() {
        DefaultCaret caret = (DefaultCaret)logViewer.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void update(AgentInput input, Plan.Posture posture, String situation, String log, BallPath ballPath) {

        planPosture.setText(posture.name());
        situationText.setText(situation);
        predictionTimeSeconds.setText(String.format("%.2f", predictionTime.getValue() / 1000.0));
        // ballHeightPredicted.setValue(0); // Commented out to avoid flicker. Should always be fresh anyway.
        ballHeightActual.setValue((int) (input.ballPosition.z * HEIGHT_BAR_MULTIPLIER));
        logViewer.append(log);

        gatherBallPredictionData(input, ballPath);
        Optional<Vector3> ballPredictionOption = getBallPrediction(input);
        if (ballPredictionOption.isPresent()) {
            Vector3 ballPrediction = ballPredictionOption.get();
            ballHeightPredicted.setValue((int) (ballPrediction.z * HEIGHT_BAR_MULTIPLIER));
            arenaDisplay.updateBallPrediction(ballPrediction);
        }

        updateBallHeightMaxes(input);
        updatePositionInfo(input);
        updateTacticsInfo(input);
        updateZonePlanInfo(input);
        arenaDisplay.updateInput(input);
        arenaDisplay.repaint();
    }

    private void updateBallHeightMaxes(AgentInput input) {
        // Calculate and display Ball Height Actual Max
        if (ballHeightActualMax.getValue() < ballHeightActual.getValue()) {
            ballHeightActualMax.setValue(ballHeightActual.getValue());
            actualMaxTime = input.time;
        } else if(Duration.between(input.time, actualMaxTime).abs().getSeconds() > 3) {
            ballHeightActualMax.setValue(0);
        }

        // Calculate and display Ball Height Predicted Max
        if (ballHeightPredictedMax.getValue() < ballHeightPredicted.getValue()) {
            ballHeightPredictedMax.setValue(ballHeightPredicted.getValue());
            predictedMaxTime = input.time;
        } else if(Duration.between(input.time, predictedMaxTime).abs().getSeconds() > 3) {
            ballHeightPredictedMax.setValue(0);
        }
    }

    private void gatherBallPredictionData(AgentInput input, BallPath ballPath) {
        int predictionMillis = predictionTime.getValue();
        LocalDateTime predictionTime = input.time.plus(Duration.ofMillis(predictionMillis));

        if (previousTime == null || !previousTime.equals(input.time)) {
            if (ballPath != null) {
                Optional<SpaceTimeVelocity> predictionSpace = ballPath.getMotionAt(predictionTime);
                if (predictionSpace.isPresent()) {
                    BallPrediction prediction = new BallPrediction(predictionSpace.get().getSpace(), predictionTime);
                    warehouse.addPrediction(prediction);
                }
            }
        }
    }

    private Optional<Vector3> getBallPrediction(AgentInput input) {
        Optional<BallPrediction> predictionOfNow = warehouse.getPredictionOfMoment(input.time);
        return predictionOfNow.map(ballPrediction -> ballPrediction.predictedLocation);
    }

    private void updatePositionInfo(AgentInput input) {
        blueCarPosX.setText(String.format("%.2f", input.blueCar.position.x));
        blueCarPosY.setText(String.format("%.2f", input.blueCar.position.y));
        blueCarPosZ.setText(String.format("%.2f", input.blueCar.position.z));
        orangeCarPosX.setText(String.format("%.2f", input.orangeCar.position.x));
        orangeCarPosY.setText(String.format("%.2f", input.orangeCar.position.y));
        orangeCarPosZ.setText(String.format("%.2f", input.orangeCar.position.z));
        ballPosX.setText(String.format("%.2f", input.ballPosition.x));
        ballPosY.setText(String.format("%.2f", input.ballPosition.y));
        ballPosZ.setText(String.format("%.2f", input.ballPosition.z));
    }

    private void updateTacticsInfo(AgentInput input) {
        Optional<TacticalSituation> situationOpt = TacticsTelemetry.get(input.team);

        if(situationOpt.isPresent()) {
            TacticalSituation situation = situationOpt.get();

            ownGoalFutureProximity.setText(String.format("%.2f", situation.ownGoalFutureProximity));
            distanceBallIsBehindUs.setText(String.format("%.2f", situation.distanceBallIsBehindUs));
            enemyOffensiveApproachError.setText(String.format("%.2f", situation.enemyOffensiveApproachError));
            distanceFromEnemyBackWall.setText(String.format("%.2f", situation.distanceFromEnemyBackWall));
            distanceFromEnemyCorner.setText(String.format("%.2f", situation.distanceFromEnemyCorner));
            needsDefensiveClear.setText(situation.needsDefensiveClear ? "True" : "False");
            shotOnGoalAvailable.setText(situation.shotOnGoalAvailable ? "True" : "False");
            //expectedEnemyContact.setText(situation.expectedEnemyContact.toString()); //TODO: Do something about the UI jitter this causes
            //scoredOnThreat.setText(); //TODO: Write toString function for SpaceTimeVelocity
            goForKickoff.setText(situation.goForKickoff ? "True" : "False");
            waitToClear.setText(situation.waitToClear ? "True" : "False");
            forceDefensivePosture.setText(situation.forceDefensivePosture ? "True" : "False");
        }
    }

    private void updateZonePlanInfo(AgentInput input) {
        Optional<ZonePlan> zonePlanOpt = ZoneTelemetry.get(input.team);

        if(zonePlanOpt.isPresent()) {
            ZonePlan zonePlan = zonePlanOpt.get();
            Zone blueCarZone = input.team == Bot.Team.BLUE ? zonePlan.myZone : zonePlan.opponentZone;
            Zone orangeCarZone = input.team == Bot.Team.ORANGE ? zonePlan.myZone : zonePlan.opponentZone;

            blueMainZone.setText(blueCarZone.mainZone.name());
            blueSubZone.setText(blueCarZone.subZone.name());
            orangeMainZone.setText(orangeCarZone.mainZone.name());
            orangeSubZone.setText(orangeCarZone.subZone.name());
            ballMainZone.setText(zonePlan.ballZone.mainZone.name());
            ballSubZone.setText(zonePlan.ballZone.subZone.name());
        }
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
