package tarehart.rlbot

import rlbot.Bot
import rlbot.cppinterop.RLBotDll
import rlbot.manager.BotManager
import rlbot.manager.PyAdapterManager
import rlbot.pyinterop.DefaultPythonInterface
import tarehart.rlbot.bots.*
import tarehart.rlbot.ui.StatusSummary
import java.io.IOException

/**
 * The public methods of this class will be called directly from the python component of the RLBot framework.
 */
class PyInterface(private val botManager: BotManager, private val statusSummary: StatusSummary) : DefaultPythonInterface(botManager) {

    override fun initBot(index: Int, botType: String, team: Int): Bot {
        val newBot: tarehart.rlbot.bots.BaseBot
        val teamEnum = AgentInput.teamFromInt(team)

        if (botType.startsWith("JumpingBean")) {
            newBot = JumpingBeanBot(teamEnum, index)
        } else if (botType.startsWith("AdversityBot")) {
            newBot = AdversityBot(teamEnum, index)
        } else if (botType.startsWith("Air Bud")) {
            newBot = AirBudBot(teamEnum, index)
        } else if (botType.startsWith("TargetBot")) {
            newBot = TargetBot(teamEnum, index)
        } else if (botType.startsWith("CarryBot")) {
            newBot = CarryBot(teamEnum, index)
        } else {
            newBot = ReliefBot(teamEnum, index)
        }
        statusSummary.markTeamRunning(teamEnum, index, newBot.debugWindow, newBot.detailFlagsPanel)

        return newBot
    }

    override fun retireBot(index: Int) {
        super.retireBot(index)

        statusSummary.removeBot(index)
    }

    override fun shutdown() {
        super.shutdown()
        pyAdapterManager.shutdown()
    }

    fun ensureDllInitialized(interfaceDllPath: String) {
        try {
            RLBotDll.initialize(interfaceDllPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    val pyAdapterManager = PyAdapterManager()

    fun registerPyAdapter(index: Int, name: String, team: Int) {
        pyAdapterManager.registerPyAdapter(index) { idx -> initBot(idx, name, team) }
    }

    fun getOutput(index: Int, secondsElapsed: Float): Array<Float> {
        val floats = Array(10) { _ -> 0F }

        val output = pyAdapterManager.getOutput(index, secondsElapsed)

        output?.let{
            floats[0] = it.throttle.toFloat()
            floats[1] = it.steer.toFloat()
            floats[2] = it.pitch.toFloat()
            floats[3] = it.yaw.toFloat()
            floats[4] = it.roll.toFloat()
            floats[5] = if (it.jumpDepressed) 1F else 0F
            floats[6] = if (it.boostDepressed) 1F else 0F
            floats[7] = if (it.slideDepressed) 1F else 0F
        }

        return floats
    }
}
