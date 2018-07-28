package tarehart.rlbot.ui;

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

public class PlainReadout {

    private static final int HEIGHT_BAR_MULTIPLIER = 10;

    private JLabel planPosture;
    private JProgressBar ballHeightActual;
    private JProgressBar ballHeightPredicted;
    private JSlider predictionTime;
    private JPanel rootPanel;
    private JProgressBar ballHeightActualMax;
    private JProgressBar ballHeightPredictedMax;
    private JLabel situationText;
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


    public PlainReadout() {
        setupUi();
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
        return zone.getMainZone() + " " + zone.getSubZone();
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
    private Font getFont(String fontName, int style, int size, Font currentFont) {
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
        setupUi();
    }

    private void setupUi() {
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setMinimumSize(new Dimension(400, 600));

        situationText = new JLabel();
        JPanel situationPanel = new JPanel();
        situationPanel.add(situationText);
        rootPanel.add(situationPanel, BorderLayout.NORTH);

        arenaDisplay = new ArenaDisplay();
        arenaDisplay.setBackground(new Color(0xFCFCFC));
        rootPanel.add(arenaDisplay, BorderLayout.CENTER);
        final JPanel diagnostics = new JPanel();
        diagnostics.setLayout(new BoxLayout(diagnostics, BoxLayout.Y_AXIS));
        rootPanel.add(diagnostics, BorderLayout.EAST);
        final JScrollPane scrollPane = new JScrollPane();
        diagnostics.add(scrollPane);
        logViewer = new JTextArea();
        logViewer.setEditable(false);
        scrollPane.setViewportView(logViewer);
        scrollPane.setMaximumSize(new Dimension(400, 600));
        scrollPane.setPreferredSize(new Dimension(400, 600));
        diagnostics.add(new JLabel("Log Filter"));
        logFilter = new JTextField();
        diagnostics.add(logFilter);
        final JPanel extras = new JPanel();
        extras.setLayout(new GridLayout(0, 2));
        diagnostics.add(extras);
        extras.add(new JLabel("Posture"));
        planPosture = new JLabel("NEUTRAL");
        extras.add(planPosture);
        extras.add(new JLabel("Ball Height Actual"));
        ballHeightActual = new JProgressBar();
        ballHeightActual.setMaximum(450);
        extras.add(ballHeightActual);

        extras.add(new JLabel("Ball Height A. Max"));
        ballHeightActualMax = new JProgressBar();
        ballHeightActualMax.setMaximum(450);
        extras.add(ballHeightActualMax);

        extras.add(new JLabel("Ball Height Predicted"));
        ballHeightPredicted = new JProgressBar();
        ballHeightPredicted.setMaximum(450);
        extras.add(ballHeightPredicted);

        extras.add(new JLabel("Ball Height P. Max"));
        ballHeightPredictedMax = new JProgressBar();
        ballHeightPredictedMax.setMaximum(450);
        extras.add(ballHeightPredictedMax);

        extras.add(new JLabel("Prediction Time (s)"));
        final JPanel predictionSliderPanel = new JPanel(new FlowLayout());
        extras.add(predictionSliderPanel);
        predictionTime = new JSlider();
        predictionTime.setMaximum(5000);
        predictionTime.setPaintTicks(true);
        predictionTime.setValue(1000);
        predictionSliderPanel.add(predictionTime);
        predictionTimeSeconds = new JLabel();
        predictionTimeSeconds.setText("1.000000");
        predictionSliderPanel.add(predictionTimeSeconds);



        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout());
        diagnostics.add(panel4);
        omniText = new JTextPane();
        omniText.setBackground(new Color(-1));
        Font omniTextFont = this.getFont("Monospaced", -1, -1, omniText.getFont());
        if (omniTextFont != null) omniText.setFont(omniTextFont);
        //panel4.add(omniText);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayout(0, 2));
        rootPanel.add(panel5, BorderLayout.WEST);
        final JLabel label9 = new JLabel();
        label9.setText("ownGoalFutureProximity");
        panel5.add(label9);
        ownGoalFutureProximity = new JLabel();
        ownGoalFutureProximity.setText("?");
        panel5.add(ownGoalFutureProximity);
        final JLabel label10 = new JLabel();
        label10.setText("distanceBallIsBehindUs");
        panel5.add(label10);
        distanceBallIsBehindUs = new JLabel();
        distanceBallIsBehindUs.setText("?");
        panel5.add(distanceBallIsBehindUs);
        final JLabel label11 = new JLabel();
        label11.setText("enemyOffensiveApproachError");
        panel5.add(label11);
        enemyOffensiveApproachError = new JLabel();
        enemyOffensiveApproachError.setText("?");
        panel5.add(enemyOffensiveApproachError);
        final JLabel label12 = new JLabel();
        label12.setText("distanceFromEnemyBackWall");
        panel5.add(label12);
        distanceFromEnemyBackWall = new JLabel();
        distanceFromEnemyBackWall.setText("?");
        panel5.add(distanceFromEnemyBackWall);
        final JLabel label13 = new JLabel();
        label13.setText("expectedEnemyContact");
        panel5.add(label13);
        expectedEnemyContact = new JLabel();
        expectedEnemyContact.setText("?");
        panel5.add(expectedEnemyContact);
        final JLabel label14 = new JLabel();
        label14.setText("scoredOnThreat");
        panel5.add(label14);
        scoredOnThreat = new JLabel();
        scoredOnThreat.setText("?");
        panel5.add(scoredOnThreat);

        final JLabel label15 = new JLabel();
        label15.setText("distanceFromEnemyCorner");
        panel5.add(label15);
        distanceFromEnemyCorner = new JLabel();
        distanceFromEnemyCorner.setText("?");
        panel5.add(distanceFromEnemyCorner);

        panel5.add(new JLabel("waitToClear"));
        waitToClear = new JLabel();
        waitToClear.setText("?");
        panel5.add(waitToClear);

        panel5.add(new JLabel("forceDefensivePosture"));
        forceDefensivePosture = new JLabel();
        forceDefensivePosture.setText("?");
        panel5.add(forceDefensivePosture);

        panel5.add(new JLabel("shotOnGoalAvailable"));
        shotOnGoalAvailable = new JLabel();
        shotOnGoalAvailable.setText("?");
        panel5.add(shotOnGoalAvailable);

        panel5.add(new JLabel("needsDefensiveClear"));
        needsDefensiveClear = new JLabel();
        needsDefensiveClear.setText("?");
        panel5.add(needsDefensiveClear);

        panel5.add(new JLabel("goForKickoff"));
        goForKickoff = new JLabel();
        goForKickoff.setText("?");
        panel5.add(goForKickoff);
    }

    /**
     * @noinspection ALL
     */
    public JComponent getRootComponent() {
        return rootPanel;
    }

}
