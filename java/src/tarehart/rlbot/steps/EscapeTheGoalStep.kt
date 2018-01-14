package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.TacticsTelemetry
import java.awt.Graphics2D
import java.util.*

class EscapeTheGoalStep : Step {

    override val situation = "Escaping the goal"

    override fun getOutput(input: AgentInput): Optional<AgentOutput> {

        val car = input.myCarData
        if (!ArenaModel.isBehindGoalLine(car.position)) {
            return Optional.empty()
        }

        val target = TacticsTelemetry.get(car.playerIndex)?.futureBallMotion?.space ?: Vector3()
        val toTarget = target.minus(car.position)
        val nearestGoal = GoalUtil.getNearestGoal(car.position)
        val goalPlane = nearestGoal.threatPlane
        val desiredExit = VectorUtil.getPlaneIntersection(goalPlane, car.position, toTarget) ?: nearestGoal.center

        val exit = nearestGoal.getNearestEntrance(desiredExit, 2.0)

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, exit).withBoost(false))
    }

    override fun canInterrupt(): Boolean {
        return true
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
