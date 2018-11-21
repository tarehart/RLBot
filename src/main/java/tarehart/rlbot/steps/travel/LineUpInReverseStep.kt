package tarehart.rlbot.steps.travel

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.steps.StandardStep

class LineUpInReverseStep(private val waypoint: Vector2) : StandardStep() {
    private var correctionDirection: Int? = null

    override val situation: String
        get() = "Lining up in reverse"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.myCarData

        val waypointToCar = car.position.flatten().minus(waypoint)

        val correctionRadians = car.orientation.noseVector.flatten().correctionAngle(waypointToCar)

        if (correctionDirection == null) {
            correctionDirection = BotMath.nonZeroSignum(correctionRadians)
        }

        val futureRadians = correctionRadians + car.spin.yawRate * .3

        return if (futureRadians * correctionDirection!! < 0 && Math.abs(futureRadians) < Math.PI / 4) {
            null // Done orienting.
        } else AgentOutput().withThrottle(-1.0).withSteer(correctionDirection!!.toDouble())

    }
}
