package tarehart.rlbot.steps.spikerush

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.tactics.SpikeRushTacticsAdvisor
import tarehart.rlbot.time.Duration
import java.lang.Float.max

class SpikeCarryStep : NestedPlanStep() {
    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        val car = bundle.agentInput.myCarData

        val distance = car.position.distance(bundle.agentInput.ballPosition)

        if (distance > SpikeRushTacticsAdvisor.SPIKED_DISTANCE) {
            return null
        }

        val ballRelative = car.relativePosition(bundle.agentInput.ballPosition).normaliseCopy()
        val scorePosition = GoalUtil.getEnemyGoal(car.team).getNearestEntrance(car.position, 2.0).flatten()

        if (ballRelative.z > .7 && SteerUtil.isDrivingOnTarget(car, scorePosition) && Math.abs(car.spin.yawRate) < 0.1
                && bundle.tacticalSituation.shotOnGoalAvailable) {

            val yawRate = Clamper.clamp(ballRelative.y * -.005, -1, 1)
            val pitchDownSeconds = 0.03 + max(0f, 3 - ballRelative.x) * 0.06

            return startPlan(Plan().withStep(BlindSequence()
                    .withStep(BlindStep(Duration.ofMillis(390), AgentOutput().withJump().withPitch(1.0).withYaw(yawRate)))
                    .withStep(BlindStep(Duration.ofMillis(50), AgentOutput()))
                    .withStep(BlindStep(Duration.ofSeconds(pitchDownSeconds), AgentOutput().withPitch(-1.0).withJump()))
                    .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withUseItem()))), bundle)
        }

        return SteerUtil.steerTowardGroundPosition(car, scorePosition, false)
    }

    override fun getLocalSituation(): String {
        return "Carrying spiked ball!"
    }
}
