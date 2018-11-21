package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.TacticsTelemetry

class EscapeTheGoalStep : StandardStep() {

    override val situation = "Escaping the goal"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        if (!ArenaModel.isBehindGoalLine(car.position)) {
            return null
        }

        val target = TacticsTelemetry.get(car.playerIndex)?.futureBallMotion?.space ?: Vector3()
        val toTarget = target.minus(car.position)
        val nearestGoal = GoalUtil.getNearestGoal(car.position)
        val goalPlane = Plane(nearestGoal.scorePlane.normal, nearestGoal.scorePlane.position * 0.98)
        val desiredExit = VectorUtil.getPlaneIntersection(goalPlane, car.position, toTarget) ?: nearestGoal.center

        val exit = nearestGoal.getNearestEntrance(desiredExit, 2.0)

        return SteerUtil.steerTowardGroundPosition(car, exit).withBoost(false)
    }
}
