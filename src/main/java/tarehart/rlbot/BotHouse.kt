package tarehart.rlbot

import rlbot.manager.BotManager
import tarehart.rlbot.bots.BaseBot
import tarehart.rlbot.ui.DisplayFlags
import tarehart.rlbot.ui.ScreenResolution
import java.awt.BorderLayout
import java.awt.Component.CENTER_ALIGNMENT
import java.awt.Font
import java.awt.Toolkit
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EmptyBorder


class BotHouse {

    private val bots: MutableMap<Int, BaseBot> = HashMap()

    fun start(port: Int) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ScreenResolution.init()
        DisplayFlags.init()

        val botManager = BotManager()
        val pythonInterface = PyInterface(port, botManager, bots)
        Thread { pythonInterface.start() }.start()

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

        val debugCheckbox = JCheckBox("Draw debug lines")
        debugCheckbox.font = font
        debugCheckbox.addActionListener { debugMode = debugCheckbox.isSelected }
        dataPanel.add(debugCheckbox)

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
            botsRunning.text = "Bots running: $botsStr"
        }

        Timer(1000, myListener).start()
    }

    companion object {
        var debugMode = false
    }
}
