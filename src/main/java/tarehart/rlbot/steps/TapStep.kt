package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.time.GameTime

class TapStep(private val numFrames: Int, private val output: AgentOutput) : StandardStep() {
    private var frameCount: Int = 0
    private var previousTime: GameTime? = null

    override val situation = "Muscle memory"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        if (previousTime == null || bundle.time.isAfter(previousTime!!)) {
            frameCount++
            previousTime = bundle.time
        }

        return if (frameCount > numFrames) {
            null
        } else output
    }

    override fun canInterrupt(): Boolean {
        return false
    }
}
