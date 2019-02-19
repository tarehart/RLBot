package tarehart.rlbot.ui

import tarehart.rlbot.bots.Team
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class StatusLineItem(team: Team, val playerIndex: Int, private val detailsPanel: JFrame) : JPanel() {
    private val botDescription = JLabel()
    private val detailsButton = JButton()
    private val flagSimplePlan = JButton()
    private val enabledColor = Color(152,251,152)
    private val disabledColor = JButton().background

    init {

        this.layout = FlowLayout()
        this.isEnabled = false

        this.add(botDescription)
        this.add(detailsButton)
        this.add(flagSimplePlan)

        botDescription.text = "Player " + playerIndex + " - " + team.name.toLowerCase()
        detailsButton.text = "Details"
        flagSimplePlan.text = "Simple Plan"

        flagSimplePlan.isContentAreaFilled = false
        flagSimplePlan.isOpaque = true

        flagSimplePlan.background = if(DisplayFlags[DisplayFlags.SIMPLE_PLAN] == 1) enabledColor else disabledColor

        this.background = if (team === Team.BLUE) Color(187, 212, 255) else Color(250, 222, 191)
        detailsButton.addActionListener { showDebugForm() }
        flagSimplePlan.addActionListener { toggleSimplePlan() }
    }

    private fun showDebugForm() {
        detailsPanel.pack()
        detailsPanel.isVisible = true
    }

    private fun toggleSimplePlan() {
        val flagVal = DisplayFlags[DisplayFlags.SIMPLE_PLAN]
        DisplayFlags[DisplayFlags.SIMPLE_PLAN] = if(flagVal == 1) 0 else 1
        flagSimplePlan.background = if(flagVal == 1) enabledColor else disabledColor
    }
}