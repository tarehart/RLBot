package tarehart.rlbot.hoops.tacticalstate

import rlbot.render.NamedRenderer
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.rendering.RenderUtil
import java.awt.Color

class IdleState : TacticalState {

    val idleRenderer = NamedRenderer("hoopsIdleRenderer")

    override fun muse(bundle: TacticalBundle): TacticalState {
        val situation = bundle.tacticalSituation
        val input = bundle.agentInput
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

    override fun urgentPlan(bundle: TacticalBundle, currentPlan: Plan?) : Plan?{
        return null
    }

    override fun newPlan(bundle: TacticalBundle) : Plan {
        // println("I don't know what to do, empty plan for me")
        return Plan()
    }
}