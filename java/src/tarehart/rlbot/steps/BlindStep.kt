package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.awt.*
import java.util.Optional

class BlindStep(private val duration: Duration, private val output: AgentOutput) : Step {
    private lateinit var scheduledEndTime: GameTime

    override val situation = "Muscle memory"

    override fun getOutput(input: AgentInput): Optional<AgentOutput> {
        if (! ::scheduledEndTime.isInitialized) {
            scheduledEndTime = input.time.plus(duration)
        }

        if (input.time.isAfter(scheduledEndTime)) {
            return Optional.empty()
        }

        return Optional.of(output)
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
