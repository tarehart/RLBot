package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.steps.Step
import java.util.*

open class Plan @JvmOverloads constructor(val posture: Posture = Posture.NEUTRAL, val label: String? = null) {
    private var unstoppable: Boolean = false
    protected var steps = ArrayList<Step>()
    protected var currentStepIndex = 0
    protected var canceled = false

    /**
     * You should make sure the plan is not complete before calling this.
     */
    open val currentStep: Step
        get() = steps[currentStepIndex]

    open val situation: String
        get() {
            val labelOrEmpty = label ?: ""
            return if (isComplete()) {
                "$labelOrEmpty (Dead plan)"
            } else "${posture.name} $labelOrEmpty ${currentStepIndex + 1}. ${currentStep.situation}"
        }

    fun canInterrupt(): Boolean {
        return isComplete() || !unstoppable && currentStep.canInterrupt()
    }

    fun withStep(step: Step): Plan {
        steps.add(step)
        return this
    }

    fun withSteps(plan: Plan): Plan {
        steps.addAll(plan.steps)
        return this
    }

    fun unstoppable(): Plan {
        this.unstoppable = true
        return this
    }

    open fun getOutput(bundle: TacticalBundle): AgentOutput? {

        if (isComplete()) {
            return null
        }

        while (currentStepIndex < steps.size) {

            val output = currentStep.getOutput(bundle)

            output?.let { return it }

            if (currentStep.getPlanGuidance() == PlanGuidance.CANCEL) {
                canceled = true
                return null
            }

            nextStep()
        }

        return null
    }

    protected fun nextStep() {
        currentStepIndex++
    }

    open fun isComplete(): Boolean {
        return canceled || currentStepIndex >= steps.size
    }

    companion object {

        fun concatSituation(baseSituation: String, plan: Plan?): String {
            return baseSituation + if (plan != null && !plan.isComplete()) "(" + plan.situation + ")" else ""
        }

        fun activePlanKt(plan: Plan?): Plan? {
            return if (plan != null && !plan.isComplete()) {
                plan
            } else null
        }
    }
}
