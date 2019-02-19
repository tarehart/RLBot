package tarehart.rlbot.ui

import tarehart.rlbot.bots.Team
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class StatusLineItem(team: Team, val playerIndex: Int, private val detailsPanel: JFrame, private val displayFlagsPanel: DisplayFlagsFrame) : JPanel() {
    private val botDescription = JLabel()
    private val detailsButton = JButton()
    private val displayFlagsButton = JButton()
    private val flagAll = JButton()

    init {

        this.layout = FlowLayout()
        this.isEnabled = false

        // Add all of the buttons to the line for this bot
        this.add(botDescription)
        this.add(detailsButton)
        this.add(displayFlagsButton)
        this.add(flagAll)

        // Setup the names of all of the buttons
        botDescription.text = "Player " + playerIndex + " - " + team.name.toLowerCase()
        detailsButton.text = "Details"
        displayFlagsButton.text = "Display Config"
        flagAll.text = "Toggle All"

        this.background = if (team === Team.BLUE) Color(187, 212, 255) else Color(250, 222, 191)

        // Add action listeners for all of the buttons
        detailsButton.addActionListener { showDebugForm() }
        displayFlagsButton.addActionListener { showDetailsConfigForm() }
        flagAll.addActionListener { toggleAllFlags() }
    }

    private fun showDebugForm() {
        detailsPanel.pack()
        detailsPanel.isVisible = true
    }

    private fun showDetailsConfigForm() {
        displayFlagsPanel.pack()
        displayFlagsPanel.isVisible = true
    }

    private fun toggleAllFlags() {
        DisplayFlags.toggleAllFlags()
        displayFlagsPanel.updateColors()
    }
}
