package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.steps.Step
import java.awt.Color
import java.util.*

open class Plan @JvmOverloads constructor(val posture: Posture = Posture.NEUTRAL) {
    private var unstoppable: Boolean = false
    protected var steps = ArrayList<Step>()
    protected var currentStepIndex = 0
    protected var canceled = false

    /**
     * You should make sure the plan is not complete before calling this.
     */
    val currentStep: Step
        get() = steps[currentStepIndex]

    open val situation: String
        get() = if (isComplete()) {
            "Dead plan"
        } else posture.name + " " + (currentStepIndex + 1) + ". " + currentStep.situation

    fun canInterrupt(): Boolean {
        return isComplete() || !unstoppable && currentStep.canInterrupt()
    }

    enum class Posture constructor(private val urgency: Int, val color: Color) {
        NEUTRAL(0, Color.WHITE),
        OFFENSIVE(1, Color.RED),
        DEFENSIVE(5, Color.GREEN),
        CLEAR(8, Color.CYAN),
        ESCAPEGOAL(8, Color.WHITE),
        SAVE(10, Color.CYAN),
        LANDING(15, Color.WHITE),
        SPIKE_CARRY(40, Color.RED),
        KICKOFF(50, Color.RED),
        MENU(75, Color.GRAY),
        OVERRIDE(100, Color.BLUE);

        fun lessUrgentThan(other: Posture): Boolean {
            return urgency < other.urgency
        }

        fun canInterrupt(plan: Plan?): Boolean {
            return plan?.let { it.isComplete() || it.posture.lessUrgentThan(this) && it.canInterrupt() } ?: true
        }
    }

    fun withStep(step: Step): Plan {
        steps.add(step)
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
