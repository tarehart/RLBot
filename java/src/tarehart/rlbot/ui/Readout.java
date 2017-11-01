package tarehart.rlbot.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.tuning.BallPrediction;
import tarehart.rlbot.tuning.PredictionWarehouse;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
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
    private JProgressBar ballHeightActualMax;
    private JProgressBar ballHeightPredictedMax;
    private JTextPane situationText;
    private JLabel predictionTimeSeconds;
    private JTextArea logViewer;
    private JLabel ownGoalFutureProximity;
    private JLabel distanceBallIsBehindUs;
    private JLabel enemyOffensiveApproachError;
    private JLabel distanceFromEnemyBackWall;
    private JLabel distanceFromEnemyCorner;
    private JLabel needsDefensiveClear;
    private JLabel shotOnGoalAvailable;
    private JLabel expectedEnemyContact;
    private JLabel scoredOnThreat;
    private ArenaDisplay arenaDisplay;
    private JLabel goForKickoff;
    private JLabel waitToClear;
    private JLabel forceDefensivePosture;
    private JTextPane omniText;

    private double maxCarSpeedVal;

    private LocalDateTime actualMaxTime = LocalDateTime.now();
    private LocalDateTime predictedMaxTime = LocalDateTime.now();

    private PredictionWarehouse warehouse = new PredictionWarehouse();

    private LocalDateTime previousTime = null;


    public Readout() {
        DefaultCaret caret = (DefaultCaret) logViewer.getCaret();
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
        updateTacticsInfo(input);
        updateOmniText(input);
        arenaDisplay.updateInput(input);
        arenaDisplay.repaint();
    }

    private void updateBallHeightMaxes(AgentInput input) {
        // Calculate and display Ball Height Actual Max
        if (ballHeightActualMax.getValue() < ballHeightActual.getValue()) {
            ballHeightActualMax.setValue(ballHeightActual.getValue());
            actualMaxTime = input.time;
        } else if (Duration.between(input.time, actualMaxTime).abs().getSeconds() > 3) {
            ballHeightActualMax.setValue(0);
        }

        // Calculate and display Ball Height Predicted Max
        if (ballHeightPredictedMax.getValue() < ballHeightPredicted.getValue()) {
            ballHeightPredictedMax.setValue(ballHeightPredicted.getValue());
            predictedMaxTime = input.time;
        } else if (Duration.between(input.time, predictedMaxTime).abs().getSeconds() > 3) {
            ballHeightPredictedMax.setValue(0);
        }
    }

    private void gatherBallPredictionData(AgentInput input, BallPath ballPath) {
        int predictionMillis = predictionTime.getValue();
        LocalDateTime predictionTime = input.time.plus(Duration.ofMillis(predictionMillis));

        if (previousTime == null || !previousTime.equals(input.time)) {
            if (ballPath != null) {
                Optional<BallSlice> predictionSpace = ballPath.getMotionAt(predictionTime);
                if (predictionSpace.isPresent()) {
                    BallPrediction prediction = new BallPrediction(predictionSpace.get().getSpace(), predictionTime, ballPath);
                    warehouse.addPrediction(prediction);
                }
                previousTime = input.time;
            }
        }
    }

    private Optional<Vector3> getBallPrediction(AgentInput input) {
        Optional<BallPrediction> predictionOfNow = warehouse.getPredictionOfMoment(input.time);
        return predictionOfNow.map(ballPrediction -> ballPrediction.predictedLocation);
    }

    private void updateOmniText(AgentInput input) {

        Optional<ZonePlan> zonePlanOpt = ZoneTelemetry.get(input.team);

        String text = "" +
                "Blue Pos: " + input.blueCar.map(c -> c.position).orElse(new Vector3()) + "\n" +
                "Orng Pos: " + input.orangeCar.map(c -> c.position).orElse(new Vector3()) + "\n" +
                "Ball Pos: " + input.ballPosition + "\n" +
                "\n" +
                "Blue Zone: " + zonePlanOpt.map(zp -> printCarZone(zp, input.team == Bot.Team.BLUE)).orElse("Unknown") + "\n" +
                "Orng Zone: " + zonePlanOpt.map(zp -> printCarZone(zp, input.team == Bot.Team.ORANGE)).orElse("Unknown") + "\n" +
                "Ball Zone: " + zonePlanOpt.map(zp -> zp.ballZone.toString()).orElse("Unknown") + "\n" +
                "\n" +
                "Ball Spin: " + input.ballSpin + "\n";

        this.omniText.setText(text);
    }

    private String printCarZone(ZonePlan zp, boolean ownZone) {
        Zone zone = ownZone ? zp.myZone :
                zp.opponentZone.isPresent() ? zp.opponentZone.get() : new Zone(Zone.MainZone.NONE, Zone.SubZone.NONE);
        return zone.mainZone + " " + zone.subZone;
    }

    private void updateTacticsInfo(AgentInput input) {
        Optional<TacticalSituation> situationOpt = TacticsTelemetry.get(input.team);

        if (situationOpt.isPresent()) {
            TacticalSituation situation = situationOpt.get();

            ownGoalFutureProximity.setText(String.format("%.2f", situation.ownGoalFutureProximity));
            distanceBallIsBehindUs.setText(String.format("%.2f", situation.distanceBallIsBehindUs));
            enemyOffensiveApproachError.setText(String.format("%.2f", situation.enemyOffensiveApproachError));
            distanceFromEnemyBackWall.setText(String.format("%.2f", situation.distanceFromEnemyBackWall));
            distanceFromEnemyCorner.setText(String.format("%.2f", situation.distanceFromEnemyCorner));
            needsDefensiveClear.setText(situation.needsDefensiveClear ? "True" : "False");
            shotOnGoalAvailable.setText(situation.shotOnGoalAvailable ? "True" : "False");
            expectedEnemyContact.setText(situation.expectedEnemyContact.space.toString());
            scoredOnThreat.setText(situation.scoredOnThreat.map(b -> b.space.toString()).orElse("None"));
            goForKickoff.setText(situation.goForKickoff ? "True" : "False");
            waitToClear.setText(situation.waitToClear ? "True" : "False");
            forceDefensivePosture.setText(situation.forceDefensivePosture ? "True" : "False");
        }
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(4, 3, new Insets(5, 5, 5, 5), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(391, 246), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Posture");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        planPosture = new JLabel();
        planPosture.setText("NEUTRAL");
        panel1.add(planPosture, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Ball Height Actual");
        panel1.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Ball Height Predicted");
        panel1.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightActual = new JProgressBar();
        ballHeightActual.setMaximum(450);
        panel1.add(ballHeightActual, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightPredicted = new JProgressBar();
        ballHeightPredicted.setMaximum(450);
        panel1.add(ballHeightPredicted, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Prediction Time (s)");
        panel1.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightPredictedMax = new JProgressBar();
        ballHeightPredictedMax.setMaximum(450);
        panel1.add(ballHeightPredictedMax, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightActualMax = new JProgressBar();
        ballHeightActualMax.setMaximum(450);
        panel1.add(ballHeightActualMax, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        predictionTime = new JSlider();
        predictionTime.setMaximum(5000);
        predictionTime.setPaintTicks(true);
        predictionTime.setValue(1000);
        panel2.add(predictionTime, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        predictionTimeSeconds = new JLabel();
        predictionTimeSeconds.setText("1.000000");
        panel2.add(predictionTimeSeconds, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Situation");
        panel1.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        situationText = new JTextPane();
        situationText.setEditable(false);
        panel1.add(situationText, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, 50), new Dimension(150, 50), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Ball Height P. Max");
        panel1.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Ball Height A. Max");
        panel1.add(label7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(391, 14), null, 0, false));
        arenaDisplay = new ArenaDisplay();
        arenaDisplay.setBackground(new Color(-263173));
        rootPanel.add(arenaDisplay, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(800, 500), null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(500, -1), new Dimension(500, -1), new Dimension(-1, 500), 0, false));
        logViewer = new JTextArea();
        logViewer.setEditable(false);
        scrollPane1.setViewportView(logViewer);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel3, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(247, 246), null, 0, false));
        omniText = new JTextPane();
        omniText.setBackground(new Color(-1));
        Font omniTextFont = this.$$$getFont$$$("Monospaced", -1, -1, omniText.getFont());
        if (omniTextFont != null) omniText.setFont(omniTextFont);
        panel3.add(omniText, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(7, 4, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("ownGoalFutureProximity");
        panel4.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ownGoalFutureProximity = new JLabel();
        ownGoalFutureProximity.setText("?");
        panel4.add(ownGoalFutureProximity, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("distanceFromEnemyCorner");
        panel4.add(label9, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        distanceFromEnemyCorner = new JLabel();
        distanceFromEnemyCorner.setText("?");
        panel4.add(distanceFromEnemyCorner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("distanceBallIsBehindUs");
        panel4.add(label10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        distanceBallIsBehindUs = new JLabel();
        distanceBallIsBehindUs.setText("?");
        panel4.add(distanceBallIsBehindUs, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("needsDefensiveClear");
        panel4.add(label11, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        needsDefensiveClear = new JLabel();
        needsDefensiveClear.setText("?");
        panel4.add(needsDefensiveClear, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("enemyOffensiveApproachError");
        panel4.add(label12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enemyOffensiveApproachError = new JLabel();
        enemyOffensiveApproachError.setText("?");
        panel4.add(enemyOffensiveApproachError, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("shotOnGoalAvailable");
        panel4.add(label13, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shotOnGoalAvailable = new JLabel();
        shotOnGoalAvailable.setText("?");
        panel4.add(shotOnGoalAvailable, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("distanceFromEnemyBackWall");
        panel4.add(label14, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        distanceFromEnemyBackWall = new JLabel();
        distanceFromEnemyBackWall.setText("?");
        panel4.add(distanceFromEnemyBackWall, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("expectedEnemyContact");
        panel4.add(label15, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        expectedEnemyContact = new JLabel();
        expectedEnemyContact.setText("?");
        panel4.add(expectedEnemyContact, new GridConstraints(5, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("scoredOnThreat");
        panel4.add(label16, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scoredOnThreat = new JLabel();
        scoredOnThreat.setText("?");
        panel4.add(scoredOnThreat, new GridConstraints(6, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        label17.setText("forceDefensivePosture");
        panel4.add(label17, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        label18.setText("goForKickoff");
        panel4.add(label18, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        label19.setText("waitToClear");
        panel4.add(label19, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        goForKickoff = new JLabel();
        goForKickoff.setText("?");
        panel4.add(goForKickoff, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        waitToClear = new JLabel();
        waitToClear.setText("?");
        panel4.add(waitToClear, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        forceDefensivePosture = new JLabel();
        forceDefensivePosture.setText("?");
        panel4.add(forceDefensivePosture, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
