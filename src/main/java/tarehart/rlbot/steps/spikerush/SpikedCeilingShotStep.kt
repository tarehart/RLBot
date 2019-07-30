package tarehart.rlbot.steps.spikerush

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.OrientationSolver
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Goal
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.tactics.SpikeRushTacticsAdvisor

class SpikedCeilingShotStep : StandardStep() {

    override val situation: String
        get() = "Carrying spiked ball!"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData

        val distance = car.position.distance(bundle.agentInput.ballPosition)

        if (distance > SpikeRushTacticsAdvisor.SPIKED_DISTANCE) {
            return null
        }

        if (!car.hasWheelContact) {
            return AgentOutput().withUseItem()
        }

        val ceiling = ArenaModel.getCollisionPlanes().first { it.normal.z == -1.0 }
        val carToGoal = GoalUtil.getEnemyGoal(car.team).center - car.position

        if (ArenaModel.isCarOnWall(car) || car.position.z > 20) {
            val targetPosition = car.position.shadowOntoPlane(ceiling) + carToGoal.flatten().scaled(0.5).withZ(ceiling.position.z)
            return SteerUtil.steerTowardPositionAcrossSeam(car, targetPosition)
        }

        val sideWalls = getViableWallPlanes()
        val targetWall = ArenaModel.getNearestPlane(car.position, sideWalls)
        val targetPosition = car.position.shadowOntoPlane(targetWall) + carToGoal.scaled(0.5).shadowOntoPlane(targetWall).withZ(ceiling.position.z)

        return SteerUtil.steerTowardPositionAcrossSeam(car, targetPosition)
    }

    companion object {
        fun getViableWallPlanes(): List<Plane> {
            return ArenaModel.getWallPlanes().filter { Math.abs(it.normal.x) > 0 }
        }
    }
}
