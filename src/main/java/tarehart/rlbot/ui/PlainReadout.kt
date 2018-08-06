package tarehart.rlbot.ui

import sun.awt.SunToolkit
import tarehart.rlbot.AgentInput
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.*
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BallPrediction
import tarehart.rlbot.tuning.PredictionWarehouse
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.text.DefaultCaret

class PlainReadout {

    private val planPosture = JLabel("NEUTRAL")
    private val ballHeightActual = JProgressBar()
    private val ballHeightPredicted = JProgressBar()
    private val predictionTime = JSlider()
    val rootPanel = JPanel(BorderLayout())
    private val ballHeightActualMax = JProgressBar()
    private val ballHeightPredictedMax = JProgressBar()
    private val situationText = JLabel()
    private val predictionTimeSeconds = JLabel()
    private val logViewer = JTextArea()
    private val ownGoalFutureProximity = JLabel()
    private val distanceBallIsBehindUs = JLabel()
    private val enemyOffensiveApproachError = JLabel()
    private val distanceFromEnemyBackWall = JLabel()
    private val distanceFromEnemyCorner = JLabel()
    private val needsDefensiveClear = JLabel()
    private val shotOnGoalAvailable = JLabel()
    private val expectedEnemyContact = JLabel()
    private val scoredOnThreat = JLabel()
    private val arenaDisplay = ArenaDisplay()
    private val goForKickoff = JLabel()
    private val waitToClear = JLabel()
    private val forceDefensivePosture = JLabel()
    private val omniText = JTextPane()
    private val logFilter = JTextField()

    private var actualMaxTime = GameTime(0)
    private var predictedMaxTime = GameTime(0)
    private val warehouse = PredictionWarehouse()
    private var previousTime: GameTime? = null

    init {
        setupUi()
        val caret = logViewer.caret as DefaultCaret
        caret.updatePolicy = DefaultCaret.ALWAYS_UPDATE
    }

    fun update(input: AgentInput, posture: Plan.Posture, situation: String, log: String, ballPath: BallPath) {

        var log = log
        
        if (!SunToolkit.getContainingWindow(rootPanel).isVisible) {
            return
        }

        planPosture.text = posture.name
        situationText.text = situation
        predictionTimeSeconds.text = String.format("%.2f", predictionTime.value / 1000.0)
        // ballHeightPredicted.setValue(0); // Commented out to avoid flicker. Should always be fresh anyway.
        ballHeightActual.value = (input.ballPosition.z * HEIGHT_BAR_MULTIPLIER).toInt()

        // Filter log before appending
        var filterValue: String? = logFilter.text
        if (filterValue != null && filterValue.isNotEmpty()) {
            val newLog = StringBuilder()
            filterValue = filterValue.toLowerCase()
            val splitLog = log.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in splitLog.indices) {
                if (splitLog[i].toLowerCase().contains(filterValue)) {
                    newLog.append(splitLog[i] + "\n")
                }
            }

            log = newLog.toString()
        }
        logViewer.append(log)

        gatherBallPredictionData(input, ballPath)
        val ballPredictionOption = getBallPrediction(input)
        if (ballPredictionOption.isPresent) {
            val ballPrediction = ballPredictionOption.get()
            ballHeightPredicted.value = (ballPrediction.z * HEIGHT_BAR_MULTIPLIER).toInt()
            arenaDisplay.updateBallPrediction(ballPrediction)
        }

        val tacSituation = TacticsTelemetry[input.playerIndex]

        if (tacSituation != null) {
            arenaDisplay.updateExpectedEnemyContact(tacSituation.expectedEnemyContact)
        }

        updateBallHeightMaxes(input)
        updateTacticsInfo(input)
        updateOmniText(input, tacSituation)
        arenaDisplay.updateInput(input)
        arenaDisplay.repaint()
    }

    private fun updateBallHeightMaxes(input: AgentInput) {
        // Calculate and display Ball Height Actual Max
        if (ballHeightActualMax.value < ballHeightActual.value) {
            ballHeightActualMax.value = ballHeightActual.value
            actualMaxTime = input.time
        } else if (Duration.between(input.time, actualMaxTime).abs().seconds > 3) {
            ballHeightActualMax.value = 0
        }

        // Calculate and display Ball Height Predicted Max
        if (ballHeightPredictedMax.value < ballHeightPredicted.value) {
            ballHeightPredictedMax.value = ballHeightPredicted.value
            predictedMaxTime = input.time
        } else if (Duration.between(input.time, predictedMaxTime).abs().seconds > 3) {
            ballHeightPredictedMax.value = 0
        }
    }

    private fun gatherBallPredictionData(input: AgentInput, ballPath: BallPath?) {
        val predictionMillis = predictionTime.value
        val predictionTime = input.time.plus(Duration.ofMillis(predictionMillis.toLong()))

        if (previousTime == null || previousTime != input.time) {
            if (ballPath != null) {
                val predictionSpace = ballPath.getMotionAt(predictionTime)
                if (predictionSpace != null) {
                    val prediction = BallPrediction(predictionSpace.space, predictionTime, ballPath)
                    warehouse.addPrediction(prediction)
                }
                previousTime = input.time
            }
        }
    }

    private fun getBallPrediction(input: AgentInput): Optional<Vector3> {
        val predictionOfNow = warehouse.getPredictionOfMoment(input.time)
        return predictionOfNow.map { ballPrediction -> ballPrediction.predictedLocation }
    }

    private fun updateOmniText(input: AgentInput, situation: TacticalSituation?) {

        val zonePlan = ZoneTelemetry[input.playerIndex]

        val enemy = situation?.enemyPlayerWithInitiative?.car

        val text = "" +
                "Our Pos: " + input.myCarData.position + "\n" +
                "Our Vel: " + input.myCarData.velocity + "\n" +
                "Enemy Pos: " + (enemy?.position ?: Vector3()) + "\n" +
                "Enemy Vel: " + (enemy?.velocity ?: Vector3()) + "\n" +
                "Ball Pos: " + input.ballPosition + "\n" +
                "\n" +
                "Our Car Zone: " + (if (zonePlan != null) printCarZone(zonePlan) else "Unknown") + "\n" +
                "Ball Zone: " + (zonePlan?.ballZone?.toString() ?: "Unknown") + "\n" +
                "\n" +
                "Ball Spin: " + input.ballSpin + "\n" +
                "\n" +
                "Our Boost: " + input.myCarData.boost

        this.omniText.text = text
    }

    private fun printCarZone(zp: ZonePlan): String {
        val zone = zp.myZone
        return zone.mainZone.toString() + " " + zone.subZone
    }

    private fun updateTacticsInfo(input: AgentInput) {
        val situation = TacticsTelemetry[input.playerIndex]

        if (situation != null) {

            ownGoalFutureProximity.text = String.format("%.2f", situation.ownGoalFutureProximity)
            distanceBallIsBehindUs.text = String.format("%.2f", situation.distanceBallIsBehindUs)
            enemyOffensiveApproachError.text = Optional.ofNullable(situation.enemyOffensiveApproachError)
                    .map { e -> String.format("%.2f", e) }.orElse("")
            distanceFromEnemyBackWall.text = String.format("%.2f", situation.distanceFromEnemyBackWall)
            distanceFromEnemyCorner.text = String.format("%.2f", situation.distanceFromEnemyCorner)
            expectedEnemyContact.text = Optional.ofNullable<Intercept>(situation.expectedEnemyContact)
                    .map<String> { contact -> contact.space.toString() }.orElse("")
            scoredOnThreat.text = Optional.ofNullable(situation.scoredOnThreat)
                    .map { (space) -> space.toString() }.orElse("None")

            needsDefensiveClear.text = ""
            needsDefensiveClear.background = if (situation.needsDefensiveClear) Color.GREEN else Color.WHITE
            needsDefensiveClear.isOpaque = situation.needsDefensiveClear

            shotOnGoalAvailable.text = ""
            shotOnGoalAvailable.background = if (situation.shotOnGoalAvailable) Color.GREEN else Color.WHITE
            shotOnGoalAvailable.isOpaque = situation.shotOnGoalAvailable

            goForKickoff.text = ""
            goForKickoff.background = if (situation.goForKickoff) Color.GREEN else Color.WHITE
            goForKickoff.isOpaque = situation.goForKickoff

            waitToClear.text = ""
            waitToClear.background = if (situation.waitToClear) Color.GREEN else Color.WHITE
            waitToClear.isOpaque = situation.waitToClear

            forceDefensivePosture.text = ""
            forceDefensivePosture.background = if (situation.forceDefensivePosture) Color.GREEN else Color.WHITE
            forceDefensivePosture.isOpaque = situation.forceDefensivePosture
        }
    }

    /**
     * @noinspection ALL
     */
    private fun getFont(fontName: String?, style: Int, size: Int, currentFont: Font?): Font? {
        if (currentFont == null) return null
        val resultName: String
        if (fontName == null) {
            resultName = currentFont.name
        } else {
            val testFont = Font(fontName, Font.PLAIN, 10)
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName
            } else {
                resultName = currentFont.name
            }
        }
        return Font(resultName, if (style >= 0) style else currentFont.style, if (size >= 0) size else currentFont.size)
    }

    init {
        setupUi()
    }

    private fun setupUi() {
        rootPanel.minimumSize = Dimension(400, 600)

        val situationPanel = JPanel()
        situationPanel.add(situationText)
        rootPanel.add(situationPanel, BorderLayout.NORTH)

        arenaDisplay.background = Color(0xFCFCFC)
        rootPanel.add(arenaDisplay, BorderLayout.CENTER)
        val diagnostics = JPanel()
        diagnostics.layout = BoxLayout(diagnostics, BoxLayout.Y_AXIS)
        rootPanel.add(diagnostics, BorderLayout.EAST)
        val scrollPane = JScrollPane()
        diagnostics.add(scrollPane)
        logViewer.isEditable = false
        scrollPane.setViewportView(logViewer)
        scrollPane.maximumSize = Dimension(400, 600)
        scrollPane.preferredSize = Dimension(400, 600)
        diagnostics.add(JLabel("Log Filter"))
        diagnostics.add(logFilter)
        val extras = JPanel()
        extras.layout = GridLayout(0, 2)
        diagnostics.add(extras)
        extras.add(JLabel("Posture"))
        extras.add(planPosture)
        extras.add(JLabel("Ball Height Actual"))
        ballHeightActual.maximum = 450
        extras.add(ballHeightActual)

        extras.add(JLabel("Ball Height A. Max"))
        ballHeightActualMax.maximum = 450
        extras.add(ballHeightActualMax)

        extras.add(JLabel("Ball Height Predicted"))
        ballHeightPredicted.maximum = 450
        extras.add(ballHeightPredicted)

        extras.add(JLabel("Ball Height P. Max"))
        ballHeightPredictedMax.maximum = 450
        extras.add(ballHeightPredictedMax)

        extras.add(JLabel("Prediction Time (s)"))
        val predictionSliderPanel = JPanel(FlowLayout())
        extras.add(predictionSliderPanel)
        predictionTime.maximum = 5000
        predictionTime.paintTicks = true
        predictionTime.value = 1000
        predictionSliderPanel.add(predictionTime)
        predictionTimeSeconds.text = "1.000000"
        predictionSliderPanel.add(predictionTimeSeconds)


        val panel4 = JPanel()
        panel4.layout = FlowLayout()
        diagnostics.add(panel4)
        omniText.background = Color(-1)
        val omniTextFont = this.getFont("Monospaced", -1, -1, omniText.font)
        if (omniTextFont != null) omniText.font = omniTextFont
        //panel4.add(omniText);
        val panel5 = JPanel()
        panel5.layout = GridLayout(0, 2)
        rootPanel.add(panel5, BorderLayout.WEST)
        val label9 = JLabel()
        label9.text = "ownGoalFutureProximity"
        panel5.add(label9)
        ownGoalFutureProximity.text = "?"
        panel5.add(ownGoalFutureProximity)
        val label10 = JLabel()
        label10.text = "distanceBallIsBehindUs"
        panel5.add(label10)
        distanceBallIsBehindUs.text = "?"
        panel5.add(distanceBallIsBehindUs)
        val label11 = JLabel()
        label11.text = "enemyOffensiveApproachError"
        panel5.add(label11)
        enemyOffensiveApproachError.text = "?"
        panel5.add(enemyOffensiveApproachError)
        val label12 = JLabel()
        label12.text = "distanceFromEnemyBackWall"
        panel5.add(label12)
        distanceFromEnemyBackWall.text = "?"
        panel5.add(distanceFromEnemyBackWall)
        val label13 = JLabel()
        label13.text = "expectedEnemyContact"
        panel5.add(label13)
        expectedEnemyContact.text = "?"
        panel5.add(expectedEnemyContact)
        val label14 = JLabel()
        label14.text = "scoredOnThreat"
        panel5.add(label14)
        scoredOnThreat.text = "?"
        panel5.add(scoredOnThreat)

        val label15 = JLabel()
        label15.text = "distanceFromEnemyCorner"
        panel5.add(label15)
        distanceFromEnemyCorner.text = "?"
        panel5.add(distanceFromEnemyCorner)

        panel5.add(JLabel("waitToClear"))
        waitToClear.text = "?"
        panel5.add(waitToClear)

        panel5.add(JLabel("forceDefensivePosture"))
        forceDefensivePosture.text = "?"
        panel5.add(forceDefensivePosture)

        panel5.add(JLabel("shotOnGoalAvailable"))
        shotOnGoalAvailable.text = "?"
        panel5.add(shotOnGoalAvailable)

        panel5.add(JLabel("needsDefensiveClear"))
        needsDefensiveClear.text = "?"
        panel5.add(needsDefensiveClear)

        panel5.add(JLabel("goForKickoff"))
        goForKickoff.text = "?"
        panel5.add(goForKickoff)
    }

    companion object {

        private const val HEIGHT_BAR_MULTIPLIER = 10
    }

}
