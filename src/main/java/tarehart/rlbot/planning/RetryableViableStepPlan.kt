package tarehart.rlbot.planning

import rlbot.render.Renderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.steps.Step
import tarehart.rlbot.steps.UnfailingStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Color
import java.awt.Point

/**
 * Composed of:
 * 1. A list of possible steps which can complete but might fail, in order of preference
 * 2. A fallback step which cannot fail
 *
 * We will find the first step capable of returning output and run it until:
 * - It succeeds (the plan will terminate)
 * - It returns null (we will begin running the fallback step)
 * - It fails and asks the plan to cancel (the plan will terminate)
 *
 * When the fallback step is running, we will also be attempting to re-engage one of the preferred steps
 * periodically.
 */
class RetryableViableStepPlan(
        posture: Posture,
        label: String,
        private val fallback: UnfailingStep,
        private val stillValid: (TacticalBundle) -> Boolean = {false}) : Plan(posture, label) {

    override val situation: String
        get() {
            if (isComplete()) {
                return "Dead plan"
            }
            return "${posture.name} RetryableViable - $label (${currentStepIndex + 1}/${steps.size}) - ${currentStep.situation}"
        }

    override fun isComplete(): Boolean {
        return canceled
    }

    private val fallbackDuration = Duration.ofMillis(500)
    private var fallbackExpiration: GameTime? = null

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        if (isComplete() || !stillValid(bundle) && currentStep.canInterrupt()) {
            return null
        }

        val expiration = fallbackExpiration
        val time = bundle.agentInput.time

        renderPlan(expiration, time, bundle.agentInput.myCarData.renderer)

        if (expiration != null) {
            if (time > expiration) {
                fallbackExpiration = null
            } else {
                return fallback.getUnfailingOutput(bundle)
            }
        }

        while (currentStepIndex < steps.size) {
            val currentStep = currentStep

            val output = currentStep.getOutput(bundle)
            if (output != null) {
                return output
            }

            when (currentStep.getPlanGuidance()) {
                PlanGuidance.CANCEL, PlanGuidance.STEP_SUCCEEDED -> {
                    canceled = true
                    return null
                }
            }

            nextStep()
        }

        steps.forEach { it.reset() }
        currentStepIndex = 0
        fallbackExpiration = time.plus(fallbackDuration)

        return fallback.getUnfailingOutput(bundle)
    }

    override val currentStep: Step
        get() {
            return if (fallbackExpiration == null && steps.size > 0) {
                steps[currentStepIndex]
            } else {
                fallback
            }
        }

    private fun renderPlan(expiration: GameTime?, time: GameTime, renderer: Renderer) {

        renderer.drawString2d(label, Color.CYAN, Point(400, 80), 1, 1)
        var text = ""
        for (step in steps + fallback) {
            if (step == currentStep) {
                text += ">> "
            } else {
                text += "   "
            }
            text += step.javaClass.simpleName + "\n"
        }
        if (expiration != null) {
            text += "#".repeat((expiration - time).millis.toInt() / 50)
        }

        renderer.drawString2d(text, Color.WHITE, Point(400, 100), 1, 1)
    }
}
