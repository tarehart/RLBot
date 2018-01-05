package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.Plan
import java.awt.Graphics2D
import java.util.*

abstract class NestedPlanStep : Step {

    private var plan : Plan? = null
    protected var zombie : Boolean = false

    protected fun startPlan(p: Plan, input: AgentInput) : Optional<AgentOutput> {
        plan = p
        return p.getOutput(input)
    }

    override fun getOutput(input: AgentInput): Optional<AgentOutput> {

        if (zombie) {
            return Optional.empty()
        }

        plan?.getOutput(input)?.let {
            if (it.isPresent) {
                return it
            }
        }

        return getUnplannedOutput(input)
    }

    abstract fun getUnplannedOutput(input: AgentInput): Optional<AgentOutput>

    abstract fun getLocalSituation() : String

    override fun canInterrupt(): Boolean {
        return plan?.canInterrupt() ?: true
    }

    override val situation: String
        get() = Plan.concatSituation(getLocalSituation(), plan)

    override fun drawDebugInfo(graphics: Graphics2D) {
        Plan.activePlanKt(plan)?.currentStep?.drawDebugInfo(graphics)
    }

}