package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput

data class StepResult(val agentOutput: AgentOutput?, val status: StepStatus = StepStatus.WORKING)

enum class StepStatus {
    WORKING, SUCCEEDED, FAILED, NULL
}