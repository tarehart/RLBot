package tarehart.rlbot.planning

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle

class FirstViableStepPlan(posture: Posture, label: String? = null) : Plan(posture, label) {

    private var frameCount = 0

    // If a step runs successfully for this number of frames, then we decide that it was viable, and commit to it.
    // When that step ends, the plan is complete, even if there were originally subsequent steps.
    private val framesTillCommitment = 6

    override val situation: String
        get() {
            val labelOrEmpty = label ?: ""
            if (isComplete()) {
                return "$labelOrEmpty (Dead plan)"
            }
            val committed = frameCount >= framesTillCommitment || currentStepIndex == steps.size - 1
            val statusText = if (committed) "committed" else "sampling"
            return "${posture.name} $labelOrEmpty | $statusText: ${currentStep.situation}"
        }

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        if (isComplete()) {
            return null
        }

        while (currentStepIndex < steps.size) {
            val currentStep = currentStep

            val output = currentStep.getOutput(bundle)
            if (output != null) {

                if (currentStepIndex < steps.size - 1) {
                    frameCount += 1
                    if (frameCount >= framesTillCommitment) {
                        // Remove the subsequent steps.
                        for (i in steps.size - 1 downTo currentStepIndex + 1) {
                            steps.removeAt(i)
                        }
                    }
                }

                return output
            }

            if (currentStep.getPlanGuidance() == PlanGuidance.CANCEL) {
                canceled = true
                return null
            }

            nextStep()
        }

        return null
    }
}
