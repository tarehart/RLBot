package tarehart.rlbot.bots

import rlbot.Bot
import rlbot.flat.GameTickPacket
import rlbot.render.NamedRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.BotHouse
import tarehart.rlbot.input.Chronometer
import tarehart.rlbot.math.RunningAverage
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.ZonePlan
import tarehart.rlbot.steps.WaitForActive
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.GameModeSniffer
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.ui.DisplayFlags
import tarehart.rlbot.ui.ScreenResolution
import java.awt.Color
import java.awt.Point
import java.time.Instant
import javax.swing.ButtonGroup
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JRadioButtonMenuItem


abstract class BaseBot(private val team: Team, protected val playerIndex: Int) : Bot {
    internal var currentPlan: Plan? = null
    private var previousPlan: Plan? = null
    internal var currentZonePlan: ZonePlan? = null
    private var previousSituation: String? = null
    internal var previousZonePlan: ZonePlan? = null

    private val chronometer = Chronometer()
    private var frameCount: Long = 0
    private var previousTime = Instant.now()
    private val planRenderer = NamedRenderer("baseBotPlanRenderer$playerIndex")
    private var isGameModeInitialized = false

    private var selfDestruct = false
    var debugMode: Boolean = false
        get() = BotHouse.debugMode
    open var loaded = false
        get() = isGameModeInitialized
    private val runningAverage = RunningAverage()


    private fun createMenuBar() : JMenuBar {
        val menuBar = JMenuBar()
        val menu = JMenu("View")
        val group = ButtonGroup()

        val defaultViewOption = JRadioButtonMenuItem("Default")
        defaultViewOption.isSelected = true
        group.add(defaultViewOption)
        menu.add(defaultViewOption)

        val performanceViewOption = JRadioButtonMenuItem("Performance")
        group.add(performanceViewOption)
        menu.add(performanceViewOption)

        menuBar.add(menu)
        return menuBar
    }

    override fun processInput(request: GameTickPacket?): AgentOutput {

        if (selfDestruct) throw ThreadDeath()

        val timeBefore = Instant.now()

        request ?: return AgentOutput()

        try {
            // Do nothing if we know nothing about our car
            if (request.playersLength() <= playerIndex || request.ball() == null) {
                return AgentOutput()
            }

            if (!isGameModeInitialized) {
                initializeGameMode()
                isGameModeInitialized = true
            }

            ArenaModel.GRAVITY = -request.gameInfo().worldGravityZ() / Vector3.PACKET_DISTANCE_TO_CLASSIC

            // RLBotDll.setGameState(GameState().withGameInfoState(GameInfoState().withWorldGravityZ(0.0001F)).buildPacket())

            val translatedInput = AgentInput(request, playerIndex, chronometer, frameCount++, this)
            val output = processInput(translatedInput)

            val elapsedMillis = java.time.Duration.between(timeBefore, Instant.now()).toMillis()
            if (elapsedMillis > 10) {
                BotLog.println("SLOW FRAME took $elapsedMillis ms!", playerIndex)
            }
            runningAverage.takeSample(elapsedMillis.toDouble())
            if (frameCount.rem(100) == 0L) {
                BotLog.println("Average frame time: %.2fms".format(runningAverage.average), playerIndex)
            }

            return output
        } catch (e: Exception) {
            e.printStackTrace()
            return AgentOutput()
        }
    }

    private fun initializeGameMode() {
        synchronized(lock) {
            val gameMode = GameModeSniffer.getGameMode()

            if (gameMode == GameMode.SOCCER) {
                ArenaModel.setSoccerWalls()
            } else if (gameMode == GameMode.DROPSHOT) {
                ArenaModel.setDropshotWalls()
            } else if (gameMode == GameMode.HOOPS) {
                ArenaModel.setHoopsWalls()
                GoalUtil.setHoopsGoals()
            }
        }
    }

    override fun getIndex(): Int {
        return playerIndex
    }

    override fun retire() {
        selfDestruct = true
    }


    fun processInput(input: AgentInput): AgentOutput {
        BotLog.setTimeStamp(input.time)
        val output: AgentOutput

        if (input.matchInfo.matchEnded || input.myCarData.isDemolished) {
            currentPlan = Plan(Plan.Posture.MENU).withStep(WaitForActive())
            output = AgentOutput()
        } else {

            if (input.matchInfo.roundActive) {
                output = getOutput(input)
            } else {
                // When the round is not active, some bots may wish to do work while still having no car control
                roundInLimbo(input)
                currentPlan = Plan(Plan.Posture.NEUTRAL).withStep(WaitForActive())
                output = AgentOutput()
            }
        }

        val posture = currentPlan?.posture ?: Plan.Posture.NEUTRAL
        val situation = currentPlan?.situation ?: ""
        if (debugMode && situation != previousSituation) {
            planRenderer.startPacket()

            if(DisplayFlags[DisplayFlags.DETAILED_PLAN] == 1) {
                planRenderer.drawString2d(situation, Color.WHITE, Point(3, 3 + 15 * playerIndex), 1, 1)
            }

            if(DisplayFlags[DisplayFlags.SIMPLE_PLAN] == 1) {
                val screenX = ScreenResolution.getX()
                val screenY = ScreenResolution.getY()
                val postureColor = Plan.PostureColor.fromPosture(posture)
                planRenderer.drawString2d(posture.name, postureColor,
                        Point((0.3 * screenX).toInt(), (0.65 * screenY + (15 * playerIndex)).toInt()),
                        1, 1)
            }

            planRenderer.finishAndSend()
            BotLog.println("[Sitch] " + situation, input.playerIndex)
        } else if (frameCount.rem(100) == 0L) {
            planRenderer.eraseFromScreen()
        }
        previousSituation = situation
        previousPlan = currentPlan

        return output
    }

    protected abstract fun getOutput(input: AgentInput): AgentOutput

    protected open fun roundInLimbo(input: AgentInput) { }

    companion object {
        val lock = Object()
    }
}
