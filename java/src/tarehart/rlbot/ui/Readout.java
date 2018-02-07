package tarehart.rlbot.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;
import tarehart.rlbot.tuning.BallPrediction;
import tarehart.rlbot.tuning.PredictionWarehouse;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
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
    private JTextField logFilter;

    private double maxCarSpeedVal;

    private GameTime actualMaxTime = new GameTime(0);
    private GameTime predictedMaxTime = new GameTime(0);
    private PredictionWarehouse warehouse = new PredictionWarehouse();
    private GameTime previousTime = null;


    public Readout() {
        DefaultCaret caret = (DefaultCaret) logViewer.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void update(AgentInput input, Plan.Posture posture, String situation, String log, BallPath ballPath) {

        planPosture.setText(posture.name());
        situationText.setText(situation);
        predictionTimeSeconds.setText(String.format("%.2f", predictionTime.getValue() / 1000.0));
        // ballHeightPredicted.setValue(0); // Commented out to avoid flicker. Should always be fresh anyway.
        ballHeightActual.setValue((int) (input.getBallPosition().getZ() * HEIGHT_BAR_MULTIPLIER));

        // Filter log before appending
        String filterValue = logFilter.getText();
        if (filterValue != null && filterValue.length() > 0) {
            StringBuilder newLog = new StringBuilder();
            filterValue = filterValue.toLowerCase();
            String[] splitLog = log.split("\n");

            for (int i = 0; i < splitLog.length; i++) {
                if (splitLog[i].toLowerCase().contains(filterValue)) {
                    newLog.append(splitLog[i] + "\n");
                }
            }

            log = newLog.toString();
        }
        logViewer.append(log);

        gatherBallPredictionData(input, ballPath);
        Optional<Vector3> ballPredictionOption = getBallPrediction(input);
        if (ballPredictionOption.isPresent()) {
            Vector3 ballPrediction = ballPredictionOption.get();
            ballHeightPredicted.setValue((int) (ballPrediction.getZ() * HEIGHT_BAR_MULTIPLIER));
            arenaDisplay.updateBallPrediction(ballPrediction);
        }

        TacticalSituation tacSituation = TacticsTelemetry.INSTANCE.get(input.getPlayerIndex());

        if (tacSituation != null) {
            arenaDisplay.updateExpectedEnemyContact(Optional.ofNullable(tacSituation.getExpectedEnemyContact()));
        }

        updateBallHeightMaxes(input);
        updateTacticsInfo(input);
        updateOmniText(input, Optional.ofNullable(tacSituation));
        arenaDisplay.updateInput(input);
        arenaDisplay.repaint();
    }

    private void updateBallHeightMaxes(AgentInput input) {
        // Calculate and display Ball Height Actual Max
        if (ballHeightActualMax.getValue() < ballHeightActual.getValue()) {
            ballHeightActualMax.setValue(ballHeightActual.getValue());
            actualMaxTime = input.getTime();
        } else if (Duration.Companion.between(input.getTime(), actualMaxTime).abs().getSeconds() > 3) {
            ballHeightActualMax.setValue(0);
        }

        // Calculate and display Ball Height Predicted Max
        if (ballHeightPredictedMax.getValue() < ballHeightPredicted.getValue()) {
            ballHeightPredictedMax.setValue(ballHeightPredicted.getValue());
            predictedMaxTime = input.getTime();
        } else if (Duration.Companion.between(input.getTime(), predictedMaxTime).abs().getSeconds() > 3) {
            ballHeightPredictedMax.setValue(0);
        }
    }

    private void gatherBallPredictionData(AgentInput input, BallPath ballPath) {
        int predictionMillis = predictionTime.getValue();
        GameTime predictionTime = input.getTime().plus(Duration.Companion.ofMillis(predictionMillis));

        if (previousTime == null || !previousTime.equals(input.getTime())) {
            if (ballPath != null) {
                BallSlice predictionSpace = ballPath.getMotionAt(predictionTime);
                if (predictionSpace != null) {
                    BallPrediction prediction = new BallPrediction(predictionSpace.getSpace(), predictionTime, ballPath);
                    warehouse.addPrediction(prediction);
                }
                previousTime = input.getTime();
            }
        }
    }

    private Optional<Vector3> getBallPrediction(AgentInput input) {
        Optional<BallPrediction> predictionOfNow = warehouse.getPredictionOfMoment(input.getTime());
        return predictionOfNow.map(ballPrediction -> ballPrediction.predictedLocation);
    }

    private void updateOmniText(AgentInput input, Optional<TacticalSituation> situation) {

        ZonePlan zonePlan = ZoneTelemetry.INSTANCE.get(input.getPlayerIndex());

        Optional<CarData> enemy = situation.map(TacticalSituation::getEnemyPlayerWithInitiative).map(CarWithIntercept::getCar);

        String text = "" +
                "Our Pos: " + input.getMyCarData().getPosition() + "\n" +
                "Our Vel: " + input.getMyCarData().getVelocity() + "\n" +
                "Enemy Pos: " + enemy.map(CarData::getPosition).orElse(new Vector3()) + "\n" +
                "Enemy Vel: " + enemy.map(CarData::getVelocity).orElse(new Vector3()) + "\n" +
                "Ball Pos: " + input.getBallPosition() + "\n" +
                "\n" +
                "Our Car Zone: " + (zonePlan != null ? printCarZone(zonePlan) : "Unknown") + "\n" +
                "Ball Zone: " + (zonePlan != null ? zonePlan.getBallZone().toString() : "Unknown") + "\n" +
                "\n" +
                "Ball Spin: " + input.getBallSpin() + "\n" +
                "\n" +
                "Our Boost: " + input.getMyCarData().getBoost();

        this.omniText.setText(text);
    }

    private String printCarZone(ZonePlan zp) {
        Zone zone = zp.getMyZone();
        return zone.mainZone + " " + zone.subZone;
    }

    private void updateTacticsInfo(AgentInput input) {
        TacticalSituation situation = TacticsTelemetry.INSTANCE.get(input.getPlayerIndex());

        if (situation != null) {

            ownGoalFutureProximity.setText(String.format("%.2f", situation.getOwnGoalFutureProximity()));
            distanceBallIsBehindUs.setText(String.format("%.2f", situation.getDistanceBallIsBehindUs()));
            enemyOffensiveApproachError.setText(Optional.ofNullable(situation.getEnemyOffensiveApproachError())
                    .map(e -> String.format("%.2f", e)).orElse(""));
            distanceFromEnemyBackWall.setText(String.format("%.2f", situation.getDistanceFromEnemyBackWall()));
            distanceFromEnemyCorner.setText(String.format("%.2f", situation.getDistanceFromEnemyCorner()));
            expectedEnemyContact.setText(Optional.ofNullable(situation.getExpectedEnemyContact())
                    .map(contact -> contact.getSpace().toString()).orElse(""));
            scoredOnThreat.setText(Optional.ofNullable(situation.getScoredOnThreat())
                    .map(b -> b.getSpace().toString()).orElse("None"));

            needsDefensiveClear.setText("");
            needsDefensiveClear.setBackground(situation.getNeedsDefensiveClear() ? Color.GREEN : Color.WHITE);
            needsDefensiveClear.setOpaque(situation.getNeedsDefensiveClear());

            shotOnGoalAvailable.setText("");
            shotOnGoalAvailable.setBackground(situation.getShotOnGoalAvailable() ? Color.GREEN : Color.WHITE);
            shotOnGoalAvailable.setOpaque(situation.getShotOnGoalAvailable());

            goForKickoff.setText("");
            goForKickoff.setBackground(situation.getGoForKickoff() ? Color.GREEN : Color.WHITE);
            goForKickoff.setOpaque(situation.getGoForKickoff());

            waitToClear.setText("");
            waitToClear.setBackground(situation.getWaitToClear() ? Color.GREEN : Color.WHITE);
            waitToClear.setOpaque(situation.getWaitToClear());

            forceDefensivePosture.setText("");
            forceDefensivePosture.setBackground(situation.getForceDefensivePosture() ? Color.GREEN : Color.WHITE);
            forceDefensivePosture.setOpaque(situation.getForceDefensivePosture());
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
        rootPanel.setLayout(new GridLayoutManager(3, 3, new Insets(5, 5, 5, 5), -1, -1));
        arenaDisplay = new ArenaDisplay();
        arenaDisplay.setBackground(new Color(-263173));
        rootPanel.add(arenaDisplay, new GridConstraints(0, 0, 3, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(550, 800), null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 400), new Dimension(-1, 400), new Dimension(-1, 400), 0, false));
        logViewer = new JTextArea();
        logViewer.setEditable(false);
        scrollPane1.setViewportView(logViewer);
        final JLabel label1 = new JLabel();
        label1.setText("Log Filter");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logFilter = new JTextField();
        panel1.add(logFilter, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(8, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(391, 246), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Posture");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        planPosture = new JLabel();
        planPosture.setText("NEUTRAL");
        panel2.add(planPosture, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Ball Height Actual");
        panel2.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Ball Height Predicted");
        panel2.add(label4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightActual = new JProgressBar();
        ballHeightActual.setMaximum(450);
        panel2.add(ballHeightActual, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightPredicted = new JProgressBar();
        ballHeightPredicted.setMaximum(450);
        panel2.add(ballHeightPredicted, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Prediction Time (s)");
        panel2.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightPredictedMax = new JProgressBar();
        ballHeightPredictedMax.setMaximum(450);
        panel2.add(ballHeightPredictedMax, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ballHeightActualMax = new JProgressBar();
        ballHeightActualMax.setMaximum(450);
        panel2.add(ballHeightActualMax, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        predictionTime = new JSlider();
        predictionTime.setMaximum(5000);
        predictionTime.setPaintTicks(true);
        predictionTime.setValue(1000);
        panel3.add(predictionTime, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        predictionTimeSeconds = new JLabel();
        predictionTimeSeconds.setText("1.000000");
        panel3.add(predictionTimeSeconds, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Situation");
        panel2.add(label6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        situationText = new JTextPane();
        situationText.setEditable(false);
        panel2.add(situationText, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, 50), new Dimension(150, 50), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Ball Height P. Max");
        panel2.add(label7, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Ball Height A. Max");
        panel2.add(label8, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(247, 246), null, 0, false));
        omniText = new JTextPane();
        omniText.setBackground(new Color(-1));
        Font omniTextFont = this.$$$getFont$$$("Monospaced", -1, -1, omniText.getFont());
        if (omniTextFont != null) omniText.setFont(omniTextFont);
        panel4.add(omniText, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(7, 4, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel5, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("ownGoalFutureProximity");
        panel5.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ownGoalFutureProximity = new JLabel();
        ownGoalFutureProximity.setText("?");
        panel5.add(ownGoalFutureProximity, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("distanceBallIsBehindUs");
        panel5.add(label10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        distanceBallIsBehindUs = new JLabel();
        distanceBallIsBehindUs.setText("?");
        panel5.add(distanceBallIsBehindUs, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("enemyOffensiveApproachError");
        panel5.add(label11, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enemyOffensiveApproachError = new JLabel();
        enemyOffensiveApproachError.setText("?");
        panel5.add(enemyOffensiveApproachError, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("distanceFromEnemyBackWall");
        panel5.add(label12, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        distanceFromEnemyBackWall = new JLabel();
        distanceFromEnemyBackWall.setText("?");
        panel5.add(distanceFromEnemyBackWall, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("expectedEnemyContact");
        panel5.add(label13, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        expectedEnemyContact = new JLabel();
        expectedEnemyContact.setText("?");
        panel5.add(expectedEnemyContact, new GridConstraints(5, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("scoredOnThreat");
        panel5.add(label14, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scoredOnThreat = new JLabel();
        scoredOnThreat.setText("?");
        panel5.add(scoredOnThreat, new GridConstraints(6, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("distanceFromEnemyCorner");
        panel5.add(label15, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        distanceFromEnemyCorner = new JLabel();
        distanceFromEnemyCorner.setText("?");
        panel5.add(distanceFromEnemyCorner, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        waitToClear = new JLabel();
        waitToClear.setText("?");
        panel5.add(waitToClear, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, 10), null, null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("waitToClear");
        panel5.add(label16, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        label17.setText("forceDefensivePosture");
        panel5.add(label17, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        label18.setText("shotOnGoalAvailable");
        panel5.add(label18, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        label19.setText("needsDefensiveClear");
        panel5.add(label19, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label20 = new JLabel();
        label20.setText("goForKickoff");
        panel5.add(label20, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        forceDefensivePosture = new JLabel();
        forceDefensivePosture.setText("?");
        panel5.add(forceDefensivePosture, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, 10), null, null, 0, false));
        shotOnGoalAvailable = new JLabel();
        shotOnGoalAvailable.setText("?");
        panel5.add(shotOnGoalAvailable, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, 10), null, null, 0, false));
        needsDefensiveClear = new JLabel();
        needsDefensiveClear.setText("?");
        panel5.add(needsDefensiveClear, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, 10), null, null, 0, false));
        goForKickoff = new JLabel();
        goForKickoff.setText("?");
        panel5.add(goForKickoff, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, 10), null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
