package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.steps.StandardStep

class SlideTillFacingStep(private val facing: Vector2, private val directionInRadians: Int) : StandardStep() {

    override val situation: String
        get() = "Sliding till facing"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val correctionRadians = car.orientation.noseVector.flatten().correctionAngle(facing)

        val futureRadians = correctionRadians + car.spin.yawRate * .3

        return if (futureRadians * directionInRadians < 0 && Math.abs(futureRadians) < Math.PI / 4) {
            null // Done orienting.
        } else AgentOutput().withThrottle(1.0).withSteer(-directionInRadians.toDouble()).withSlide()

    }
}
