package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.awt.*

class BlindStep(private val duration: Duration, private val output: AgentOutput) : StandardStep() {
    private lateinit var scheduledEndTime: GameTime

    override val situation = "Muscle memory"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {
        if (! ::scheduledEndTime.isInitialized) {
            scheduledEndTime = bundle.agentInput.time.plus(duration)
        }

        if (bundle.agentInput.time.isAfter(scheduledEndTime)) {
            return null
        }

        return output
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
