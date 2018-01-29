package tarehart.rlbot.ui

import tarehart.rlbot.Bot
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class StatusLineItem(team: Bot.Team, val playerIndex: Int, private val detailsPanel: JFrame) : JPanel() {
    private val botDescription = JLabel()
    private val detailsButton = JButton()

    init {

        this.layout = FlowLayout()
        this.isEnabled = false

        this.add(botDescription)
        this.add(detailsButton)

        botDescription.text = "Player " + playerIndex + " - " + team.name.toLowerCase()
        detailsButton.text = "Details"

        this.background = if (team === Bot.Team.BLUE) Color(187, 212, 255) else Color(250, 222, 191)
        detailsButton.addActionListener { showDebugForm() }
    }

    private fun showDebugForm() {
        detailsPanel.pack()
        detailsPanel.isVisible = true
    }
}