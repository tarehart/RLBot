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

        doInitialComputation(input)

        if (zombie || shouldCancelPlanAndAbort(input) && plan?.canInterrupt() != false) {
            return Optional.empty()
        }

        plan?.getOutput(input)?.let {
            if (it.isPresent) {
                return it
            }
        }

        return doComputationInLieuOfPlan(input)
    }

    /**
     * Initializes or updates any fields that will be needed subsequently.
     * This allows you to avoid duplicate computation, e.g. in the shouldCancelPlanAndAbort function,
     * by storing the results in a field.
     */
    open protected fun doInitialComputation(input: AgentInput) {
        // Do nothing. Feel free to override.
    }

    /**
     * Please avoid side effects. If you want to store the results of computation,
     * please use the doInitialComputation function.
     */
    open protected fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {
        return false
    }

    abstract fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput>

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