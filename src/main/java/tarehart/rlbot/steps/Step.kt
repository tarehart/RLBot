package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.PlanGuidance

import java.awt.*

interface Step {

    // Describes very briefly what's going on, for UI display
    val situation: String

//    /**
//     * This should be true if the step has never received a tactical bundle before.
//     * It may also be true if the step should be reset, e.g. if we're looping back
//     * through a plan a second time.
//     */
//    fun wantsInitialization(): Boolean
//
//    /**
//     * This will cause the step to want initialization again.
//     */
//    fun requestReinitialization()

    fun reset()

    /**
     * Return the output you want to pass to the bot.
     * If you pass Optional.empty(), you are declaring yourself to be complete.
     */
    fun getOutput(bundle: TacticalBundle): AgentOutput?

    fun canInterrupt(): Boolean

    fun getPlanGuidance(): PlanGuidance

    fun drawDebugInfo(graphics: Graphics2D)
}
