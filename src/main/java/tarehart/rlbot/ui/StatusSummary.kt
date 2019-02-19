package tarehart.rlbot.ui

import tarehart.rlbot.bots.Team
import javax.swing.*

class StatusSummary {
    val rootPanel = JPanel()
    private val portLbl = JLabel()
    private val playerPane = JPanel()

    init {
        setupUI()
    }

    fun markTeamRunning(team: Team, playerIndex: Int, debugPanel: JFrame, displayFlagsPanel: DisplayFlagsFrame) {

        val lineItem = StatusLineItem(team, playerIndex, debugPanel, displayFlagsPanel)

        var i = 0
        while (i < playerPane.componentCount) {
            if ((playerPane.getComponent(i) as StatusLineItem).playerIndex > playerIndex) {
                break
            }
            i++
        }
        playerPane.add(lineItem, null, i)
        lineItem.validate()
        playerPane.revalidate()

        val topFrame = SwingUtilities.getWindowAncestor(rootPanel) as JFrame
        topFrame.pack()
    }

    fun removeBot(index: Int) {
        for (i in 0 until playerPane.componentCount) {
            val lineItem = playerPane.getComponent(i) as StatusLineItem
            if (lineItem.playerIndex == index) {
                playerPane.remove(lineItem)
                break
            }
        }
    }

    fun setPort(port: Int) {
        portLbl.text = String.format("Port %s", port)
    }

    private fun setupUI() {
        playerPane.layout = BoxLayout(playerPane, BoxLayout.Y_AXIS)
        rootPanel.layout = BoxLayout(rootPanel, BoxLayout.Y_AXIS)
        rootPanel.add(playerPane)
        playerPane.isEnabled = true
        rootPanel.add(portLbl)
    }
}
