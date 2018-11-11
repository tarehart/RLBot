package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import java.awt.Graphics2D
import java.util.function.Predicate

class WhileConditionStep(private val predicate: Predicate<AgentInput>, private val output: AgentOutput) : StandardStep() {

    override val situation = "Reactive memory"

    override fun getOutput(input: AgentInput): AgentOutput? {

        if (!predicate.test(input)) {
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

    companion object {
        
        fun until(predicate: Predicate<AgentInput>, output: AgentOutput) : WhileConditionStep {
            return WhileConditionStep(predicate.negate(), output)
        }
    }
}