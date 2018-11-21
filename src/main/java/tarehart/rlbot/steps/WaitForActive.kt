package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle

class WaitForActive : StandardStep() {

    override val situation: String
        get() = "Idling"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {
        return if (bundle.matchInfo.roundActive) {
            null
        } else AgentOutput()
    }
}
