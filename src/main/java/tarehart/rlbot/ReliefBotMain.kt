package tarehart.rlbot

import rlbot.manager.BotManager
import rlbot.py.PythonServer
import tarehart.rlbot.ui.StatusSummary
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.WindowConstants

private const val DEFAULT_PORT = 22868
private val statusSummary = StatusSummary()

fun main(args: Array<String>) {

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Scenario: you finished your bot and submitted it to a tournament. Your opponent hard-coded the same
    // as you, and the match can't start because of the conflict. Because of this line, you can ask the
    // organizer make a file called "port.txt" in the same directory as your .jar, and put some other number in it.
    // This matches code in JavaAgent.py
    val port = readPortFromFile().orElse(DEFAULT_PORT)
    val botManager = BotManager()
    val pythonInterface = PyInterface(botManager, statusSummary)
    val pythonServer = PythonServer(pythonInterface, port)
    pythonServer.start()

    println(String.format("Python server started on port %s. Listening for RLBot commands!", port))

    showStatusSummary(port)
}


private fun readPortFromFile(): Optional<Int> {
    try {
        val lines = Files.lines(Paths.get("reliefbot-port.txt"))
        val firstLine = lines.findFirst()
        return firstLine.map{ Integer.parseInt(it) }
    } catch (e: NumberFormatException) {
        println("Failed to parse port file! Will proceed with hard-coded port number.")
        return Optional.empty()
    } catch (e: Throwable) {
        return Optional.empty()
    }

}

private fun showStatusSummary(port: Int) {

    statusSummary.setPort(port)

    val frame = JFrame("ReliefBot")
    frame.contentPane = statusSummary.rootPanel
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.pack()
    frame.isVisible = true
}
