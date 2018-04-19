package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput

import java.awt.*
import java.util.Optional

class WaitForActive : Step {

    override val situation: String
        get() = "Idling"

    override fun getOutput(input: AgentInput): AgentOutput? {
        return if (input.matchInfo.roundActive) {
            null
        } else AgentOutput()
    }

    override fun canInterrupt(): Boolean {
        return true
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
