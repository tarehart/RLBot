package tarehart.rlbot

import rlbot.BaseBot
import rlbot.manager.BotManager
import tarehart.rlbot.ui.DisplayFlags
import tarehart.rlbot.ui.ScreenResolution
import java.awt.BorderLayout
import java.awt.Component.CENTER_ALIGNMENT
import java.awt.Font
import java.awt.Toolkit
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EmptyBorder

const val DEFAULT_PORT = 22868

fun main(args: Array<String>) {

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    ScreenResolution.init()
    DisplayFlags.init()

    val port = readPortFromArgs(args) ?: DEFAULT_PORT
    val botManager = BotManager()
    val pythonInterface = PyInterface(port, botManager)
    Thread {pythonInterface.start()}.start()

    startWindow(port, botManager)
}

private fun startWindow(port: Int, botManager: BotManager) {
    val frame = JFrame("ReliefBot")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val panel = JPanel()
    val font = Font("Sans", Font.PLAIN, 12)
    panel.border = EmptyBorder(10, 10, 10, 20)
    val borderLayout = BorderLayout()
    panel.layout = borderLayout
    val dataPanel = JPanel()
    dataPanel.alignmentY = CENTER_ALIGNMENT
    dataPanel.layout = BoxLayout(dataPanel, BoxLayout.Y_AXIS)
    dataPanel.border = EmptyBorder(20, 10, 0, 0)

    val portLabel = JLabel("Listening on port $port")
    portLabel.font = Font("Sans", Font.BOLD, 14)
    dataPanel.add(portLabel)

    val stayOpenLabel = JLabel("I'm the thing controlling ReliefBot, keep me open :)")
    stayOpenLabel.font = font
    stayOpenLabel.border = EmptyBorder(0, 0, 10, 0)
    dataPanel.add(stayOpenLabel)

    val botsRunning = JLabel("Bots running: ")
    botsRunning.font = font
    dataPanel.add(botsRunning)

    panel.add(dataPanel, BorderLayout.CENTER)
    frame.add(panel)

    val url = BaseBot::class.java.classLoader.getResource("icon.png")
    val image = Toolkit.getDefaultToolkit().createImage(url)
    panel.add(JLabel(ImageIcon(image)), BorderLayout.WEST)
    frame.iconImage = image

    frame.pack()
    frame.isVisible = true

    val myListener = ActionListener {
        val runningBotIndices = botManager.runningBotIndices

        val botsStr: String
        botsStr = if (runningBotIndices.isEmpty()) {
            "None"
        } else {
            runningBotIndices.sorted().joinToString { i -> "#$i" }
        }
        botsRunning.text = "Bots indices running: $botsStr"
    }

    Timer(1000, myListener).start()
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
