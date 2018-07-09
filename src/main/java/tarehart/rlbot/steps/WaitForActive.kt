package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput

import java.awt.*
import java.util.Optional

class WaitForActive : StandardStep() {

    override val situation: String
        get() = "Idling"

    override fun getOutput(input: AgentInput): AgentOutput? {
        return if (input.matchInfo.roundActive) {
            null
        } else AgentOutput()
    }
}
