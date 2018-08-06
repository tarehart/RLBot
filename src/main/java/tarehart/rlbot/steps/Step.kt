package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.PlanGuidance

import java.awt.*

interface Step {

    // Describes very briefly what's going on, for UI display
    val situation: String

    /**
     * Return the output you want to pass to the bot.
     * If you pass Optional.empty(), you are declaring yourself to be complete.
     */
    fun getOutput(input: AgentInput): AgentOutput?

    fun canInterrupt(): Boolean

    fun getPlanGuidance(): PlanGuidance

    fun drawDebugInfo(graphics: Graphics2D)
}
