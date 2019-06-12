package tarehart.rlbot

import rlbot.manager.BotManager
import rlbot.pyinterop.PythonServer
import tarehart.rlbot.ui.DisplayFlags
import tarehart.rlbot.ui.ScreenResolution
import tarehart.rlbot.ui.StatusSummary
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.WindowConstants

const val DEFAULT_PORT = 22868
private val statusSummary = StatusSummary()

fun main(args: Array<String>) {

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    ScreenResolution.init()
    DisplayFlags.init()

    // Scenario: you finished your bot and submitted it to a tournament. Your opponent hard-coded the same
    // as you, and the match can't start because of the conflict. Because of this line, you can ask the
    // organizer make a file called "port.txt" in the same directory as your .jar, and put some other number in it.
    // This matches code in JavaAgent.py
    val port = readPortFromArgs(args) ?: DEFAULT_PORT
    val botManager = BotManager()
    val pythonInterface = PyInterface(port, botManager, statusSummary)
    Thread {pythonInterface.start()}.start()
    showStatusSummary(port)
}


fun readPortFromArgs(args: Array<String>): Int? {

    if (args.isEmpty()) {
        return null
    }
    return try {
        Integer.parseInt(args[0])
    } catch (e: NumberFormatException) {
        println("Failed to get port from arguments! Will use default.")
        null
    }
}

private fun showStatusSummary(port: Int) {

    statusSummary.setPort(port)

    val frame = JFrame("ReliefBot")
    frame.minimumSize = Dimension(400, 60)
    frame.contentPane = statusSummary.rootPanel
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.pack()
    frame.isVisible = true
}
