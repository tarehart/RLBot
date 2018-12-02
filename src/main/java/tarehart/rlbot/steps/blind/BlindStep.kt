package tarehart.rlbot.steps.blind

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.awt.*

class BlindStep(val duration: Duration, val output: AgentOutput) : StandardStep() {
    private lateinit var scheduledEndTime: GameTime
    private var outputSendCount = 0

    override val situation = "jmp: ${output.jumpDepressed}"

    /**
     * You do not need to call this in all circumstances. You can leave scheduledEndTime
     * un-set, and getOutput will automatically set it based on the duration once it gets its first
     * invocation. However, this is useful for giving you more control.
     */
    fun setEndTime(gameTime: GameTime) {
        scheduledEndTime = gameTime
    }

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {
        if (! ::scheduledEndTime.isInitialized) {
            scheduledEndTime = bundle.agentInput.time.plus(duration)
        }

        if (outputSendCount >= 2 && bundle.agentInput.time.isAfter(scheduledEndTime)) {
            // Exit if we're past the end time, but guarantee that we send the output at least
            // Twice, otherwise the game tends to drop inputs.
            return null
        }

        outputSendCount++
        return output
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
