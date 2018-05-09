package tarehart.rlbot.bots

import rlbot.FlatBot
import rlbot.flat.GameTickPacket
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.Chronometer
import tarehart.rlbot.input.SpinTracker
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.ZonePlan
import tarehart.rlbot.planning.ZoneTelemetry
import tarehart.rlbot.steps.WaitForActive
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.ui.Readout
import java.time.Instant
import java.util.*
import javax.swing.JFrame

abstract class BaseBot(private val team: Team, private val playerIndex: Int) : FlatBot {
    internal var currentPlan: Plan? = null
    private var previousPlan: Plan? = null
    internal var currentZonePlan: ZonePlan? = null
    private val readout: Readout = Readout()
    private var previousSituation: String? = null
    internal var previousZonePlan: ZonePlan? = null

    private val chronometer = Chronometer()
    private val spinTracker = SpinTracker()
    private var frameCount: Long = 0
    private var previousTime = Instant.now()

    val debugWindow: JFrame
        get() {
            val frame = JFrame("Debug - " + team.name)
            frame.contentPane = readout.rootPanel
            frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE
            frame.pack()
            return frame
        }

    override fun processInput(request: GameTickPacket?): AgentOutput {

        request ?: return AgentOutput()

        val elapsedMillis = java.time.Duration.between(previousTime, Instant.now()).toMillis()
        if (elapsedMillis > 20) {
            BotLog.println("SLOW FRAME took $elapsedMillis ms!", playerIndex)
        }
        previousTime = Instant.now()

        try {
            // Do nothing if we know nothing about our car
            if (request.playersLength() <= playerIndex) {
                return AgentOutput()
            }

            val translatedInput = AgentInput(request, playerIndex, chronometer, spinTracker, frameCount++)

            return processInput(translatedInput)
        } catch (e: Exception) {
            e.printStackTrace()
            return AgentOutput()
        }
    }

    override fun getIndex(): Int {
        return playerIndex
    }

    override fun retire() {
    }


    fun processInput(input: AgentInput): AgentOutput {
        BotLog.setTimeStamp(input.time)
        val output: AgentOutput
        var ballPath = Optional.empty<BallPath>()

        if (input.matchInfo.matchEnded || input.myCarData.isDemolished) {
            currentPlan = Plan(Plan.Posture.MENU).withStep(WaitForActive())
            output = AgentOutput()
        } else {
            ballPath = Optional.of(ArenaModel.predictBallPath(input))
            val zonePlan = ZonePlan(input)
            ZoneTelemetry.set(zonePlan, input.playerIndex)

            //        BallRecorder.recordPosition(new BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin));
            //        if (input.ballVelocity.magnitudeSquared() > 0) {
            //            Optional<BallSlice> afterBounce = ballPath.getMotionAfterWallBounce(1);
            //            // Just for data gathering / debugging.
            //            afterBounce.ifPresent(stv -> BallRecorder.startRecording(
            //                    new BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin),
            //                    stv.getTime().plusSeconds(1)));
            //        }

            if (input.matchInfo.roundActive) {
                output = getOutput(input)
            } else {
                currentPlan = Plan(Plan.Posture.NEUTRAL).withStep(WaitForActive())
                output = AgentOutput()
            }
        }

        val posture = currentPlan?.posture ?: Plan.Posture.NEUTRAL
        val situation = currentPlan?.situation ?: ""
        if (situation != previousSituation) {
            println("[Sitch] " + situation, input.playerIndex)
        }
        previousSituation = situation
        previousPlan = currentPlan
        ballPath.ifPresent { bp -> readout.update(input, posture, situation, BotLog.collect(input.playerIndex), bp) }

        return output
    }

    protected abstract fun getOutput(input: AgentInput): AgentOutput

}
