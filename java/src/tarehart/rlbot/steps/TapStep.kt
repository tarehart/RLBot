package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.time.GameTime

import java.awt.*
import java.util.Optional

class TapStep(private val numFrames: Int, private val output: AgentOutput) : Step {
    private var frameCount: Int = 0
    private var previousTime: GameTime? = null

    override val situation = "Muscle memory"

    override fun getOutput(input: AgentInput): AgentOutput? {

        if (previousTime == null || input.time.isAfter(previousTime!!)) {
            frameCount++
            previousTime = input.time
        }

        return if (frameCount > numFrames) {
            null
        } else output
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
