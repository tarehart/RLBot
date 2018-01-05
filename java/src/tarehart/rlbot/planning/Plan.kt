package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.steps.Step
import tarehart.rlbot.steps.StepResult
import tarehart.rlbot.steps.StepStatus

import java.util.ArrayList
import java.util.Optional

open class Plan @JvmOverloads constructor(val posture: Posture = Posture.NEUTRAL) {
    private var unstoppable: Boolean = false
    protected var steps = ArrayList<Step>()
    protected var currentStepIndex = 0

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

    enum class Posture constructor(private val urgency: Int) {
        NEUTRAL(0),
        OFFENSIVE(1),
        DEFENSIVE(5),
        CLEAR(8),
        ESCAPEGOAL(8),
        WAITTOCLEAR(9),
        SAVE(10),
        LANDING(15),
        KICKOFF(50),
        MENU(75),
        OVERRIDE(100);

        fun lessUrgentThan(other: Posture): Boolean {
            return urgency < other.urgency
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

    open fun getOutput(input: AgentInput): Optional<AgentOutput> {

        if (isComplete()) {
            return Optional.empty()
        }

        while (currentStepIndex < steps.size) {

            val output = currentStep.getOutput(input)

            if (output.isPresent) {
                return output
            }
            nextStep()
        }

        return Optional.empty()
    }

    protected fun nextStep() {
        currentStepIndex++
    }

    fun isComplete(): Boolean {
        return currentStepIndex >= steps.size
    }

    companion object {

        fun concatSituation(baseSituation: String, plan: Plan?): String {
            return baseSituation + if (plan != null && !plan.isComplete()) "(" + plan.situation + ")" else ""
        }

        fun activePlan(plan: Plan?): Optional<Plan> {
            return if (plan != null && !plan.isComplete()) {
                Optional.of(plan)
            } else Optional.empty()
        }

        fun activePlanKt(plan: Plan?): Plan? {
            return if (plan != null && !plan.isComplete()) {
                plan
            } else null
        }
    }
}
