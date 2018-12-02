package tarehart.rlbot.steps.blind

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep

/**
 * This class allows you to "pre-bake" the end times of a list of blind steps.
 *
 * Normally, each blind step only knows its duration, and figures out its end time
 * the first time it gets called. In a long list of them, this can cause error to
 * accumulate because there is generally ~8 milliseconds between when a blind step's
 * duration SHOULD end and when it actually gets a fresh input packet with confirmation.
 *
 * The pre-bake avoids that problem.
 */
class BlindSequence: NestedPlanStep() {
    override fun getLocalSituation(): String {
        return "blind sequence"
    }

    private var hasRun = false

    private val blindSteps = ArrayList<BlindStep>()

    fun withStep(step: BlindStep): BlindSequence {
        blindSteps.add(step)
        return this
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        if (hasRun) {
            return null
        }

        val p = Plan()
        var timeCursor = bundle.agentInput.time
        blindSteps.forEach {
            timeCursor += it.duration
            it.setEndTime(timeCursor)
            p.withStep(it)
        }

        hasRun = true
        return startPlan(p, bundle)
    }
}