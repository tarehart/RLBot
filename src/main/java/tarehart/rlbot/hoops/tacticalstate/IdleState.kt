package tarehart.rlbot.hoops.tacticalstate

import rlbot.flat.Vector3
import rlbot.render.NamedRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.BlindStep
import tarehart.rlbot.tactics.TacticalSituation
import tarehart.rlbot.time.Duration
import java.awt.Color

class IdleState : TacticalState {

    val idleRenderer = NamedRenderer("hoopsIdleRenderer")

    override fun muse(bundle: TacticalBundle, situation: TacticalSituation): TacticalState {
        if (input.ballPosition.flatten().isZero) {
            return KickoffState()
        }

        idleRenderer.startPacket()
        RenderUtil.drawPath(idleRenderer, situation.ballPath.slices.map {
            it.space
        }, Color.WHITE)
        idleRenderer.finishAndSend()
        return this
    }

    override fun urgentPlan(bundle: TacticalBundle, situation: TacticalSituation, currentPlan: Plan?) : Plan?{
        return null
    }

    override fun newPlan(bundle: TacticalBundle, situation: TacticalSituation) : Plan {
        // println("I don't know what to do, empty plan for me")
        return Plan()
    }
}