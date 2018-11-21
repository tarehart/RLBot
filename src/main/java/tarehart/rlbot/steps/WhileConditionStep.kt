package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import java.awt.Graphics2D
import java.util.function.Predicate

class WhileConditionStep(private val predicate: Predicate<AgentInput>, private val outputFn: (input: AgentInput) -> AgentOutput) : StandardStep() {

    override val situation = "Reactive memory"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        if (!predicate.test(bundle)) {
            return null
        }

        return outputFn(bundle)
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {
        
        fun until(predicate: Predicate<AgentInput>, outputFn: (input: AgentInput) -> AgentOutput) : WhileConditionStep {
            return WhileConditionStep(predicate.negate(), outputFn)
        }
    }
}