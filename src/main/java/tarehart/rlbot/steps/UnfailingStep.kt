package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.PlanGuidance

import java.awt.*

interface UnfailingStep: Step {

    fun getUnfailingOutput(bundle: TacticalBundle): AgentOutput
}
