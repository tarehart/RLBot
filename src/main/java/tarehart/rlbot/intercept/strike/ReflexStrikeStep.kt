package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import kotlin.math.max

class ReflexStrikeStep(val desiredForceDirection: Vector2, val desiredCarHeight: Float, val deadline: GameTime): NestedPlanStep() {

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        if (car.velocity.z < -1 || car.time > deadline) {
            BotLog.println("Missed our chance at a reflex strike.", car.playerIndex)
            return null
        }

        if (car.position.z >= desiredCarHeight) {
            val relativePosition = car.relativePosition(car.position + desiredForceDirection.withZ(0))
            val forward = relativePosition.x
            val left = relativePosition.y

            val max = max(forward, left)
            val pitch = forward / max
            val roll = left / max

            return startPlan(Plan().unstoppable()
                    .withStep(BlindStep(Duration.ofMillis(20), AgentOutput()))
                    .withStep(BlindStep(Duration.ofMillis(20), AgentOutput().withJump().withPitch(pitch).withRoll(roll))),
                    bundle)
        }

        return AgentOutput().withJump().withPitch(-.5)
    }

    override fun getLocalSituation(): String {
        return "Reflex Strike"
    }
}
