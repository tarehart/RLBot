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
    private val flagDetailedPlan = JButton()
    private val flagBallPath = JButton()
    private val enabledColor = Color(152,251,152)
    private val disabledColor = JButton().background

    init {

        this.layout = FlowLayout()
        this.isEnabled = false

        this.add(botDescription)
        this.add(detailsButton)
        this.add(flagSimplePlan)
        this.add(flagDetailedPlan)
        this.add(flagBallPath)

        botDescription.text = "Player " + playerIndex + " - " + team.name.toLowerCase()
        detailsButton.text = "Details"
        flagSimplePlan.text = "Simple Plan"
        flagDetailedPlan.text = "Detailed Plan"
        flagBallPath.text = "Ball Path"

        flagSimplePlan.isContentAreaFilled = false
        flagSimplePlan.isOpaque = true
        flagDetailedPlan.isContentAreaFilled = false
        flagDetailedPlan.isOpaque = true
        flagBallPath.isContentAreaFilled = false
        flagBallPath.isOpaque = true

        flagSimplePlan.background = if(DisplayFlags[DisplayFlags.SIMPLE_PLAN] == 1) enabledColor else disabledColor
        flagDetailedPlan.background = if(DisplayFlags[DisplayFlags.DETAILED_PLAN] == 1) enabledColor else disabledColor
        flagBallPath.background = if(DisplayFlags[DisplayFlags.BALL_PATH] == 1) enabledColor else disabledColor

        this.background = if (team === Team.BLUE) Color(187, 212, 255) else Color(250, 222, 191)
        detailsButton.addActionListener { showDebugForm() }
        flagSimplePlan.addActionListener { toggleSimplePlan() }
        flagDetailedPlan.addActionListener { toggleDetailedPlan() }
        flagBallPath.addActionListener { toggleBallPath() }
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

    private fun toggleDetailedPlan() {
        val flagVal = DisplayFlags[DisplayFlags.DETAILED_PLAN]
        DisplayFlags[DisplayFlags.DETAILED_PLAN] = if(flagVal == 1) 0 else 1
        flagDetailedPlan.background = if(flagVal == 1) enabledColor else disabledColor
    }

    private fun toggleBallPath() {
        val flagVal = DisplayFlags[DisplayFlags.BALL_PATH]
        DisplayFlags[DisplayFlags.BALL_PATH] = if(flagVal == 1) 0 else 1
        flagBallPath.background = if(flagVal == 1) enabledColor else disabledColor
    }
}