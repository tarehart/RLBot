package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.PlanGuidance
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.awt.*
import java.util.Optional

class BlindStep(private val duration: Duration, private val output: AgentOutput) : StandardStep() {
    private lateinit var scheduledEndTime: GameTime

    override val situation = "Muscle memory"

    override fun getOutput(input: AgentInput): AgentOutput? {
        if (! ::scheduledEndTime.isInitialized) {
            scheduledEndTime = input.time.plus(duration)
        }

        if (input.time.isAfter(scheduledEndTime)) {
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
