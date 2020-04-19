package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import kotlin.math.max

class ReflexStrikeStep(val desiredHeightOfContactPoint: Float, val deadline: GameTime): NestedPlanStep() {

    val jumpPlan = getJumpPlan()
    var startedDodge = false

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        if (car.velocity.z < -1 || car.time > deadline) {
            BotLog.println("Missed our chance at a reflex strike.", car.playerIndex)
            return null
        }

        if (startedDodge) {
            return null // Already dodged successfully.
        }

        val slice = CarSlice(car)
        val frontDot = slice.hitboxCenterWorld + slice.toNose + slice.toRoof
        val frontDotSoon = frontDot + car.velocity * UNJUMP_TIME.seconds

        if (frontDotSoon.z >= desiredHeightOfContactPoint) {
            val relativePosition = car.relativePosition(bundle.agentInput.ballPosition)
            val forward = relativePosition.x
            val left = relativePosition.y

            val max = max(forward, left)
            val pitch = -forward / max
            val roll = -left / max

            startedDodge = true

            return startPlan(Plan().unstoppable()
                    .withStep(BlindStep(UNJUMP_TIME, AgentOutput()))
                    .withStep(BlindStep(Duration.ofMillis(20), AgentOutput().withJump().withPitch(pitch).withRoll(roll))),
                    bundle)
        }

        return jumpPlan.getOutput(bundle)
    }

    override fun getLocalSituation(): String {
        return "Reflex Strike"
    }

    companion object {
        val UNJUMP_TIME = Duration.ofMillis(16)

        fun getJumpPlan(): Plan {
            return Plan()
                    .withStep(BlindStep(Duration.ofSeconds(.26), AgentOutput().withJump().withPitch(1)))
                    .withStep(BlindStep(Duration.ofSeconds(.2), AgentOutput().withJump().withPitch(-1)))
                    .withStep(BlindStep(Duration.ofSeconds(.3), AgentOutput().withJump()))
        }
    }
}
