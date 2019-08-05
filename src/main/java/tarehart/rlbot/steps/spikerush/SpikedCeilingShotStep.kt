package tarehart.rlbot.steps.spikerush

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.Plane
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.tactics.SpikeRushTacticsAdvisor
import tarehart.rlbot.time.Duration

class SpikedCeilingShotStep : NestedPlanStep() {
    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        val car = bundle.agentInput.myCarData

        val distance = car.position.distance(bundle.agentInput.ballPosition)

        if (distance > SpikeRushTacticsAdvisor.SPIKED_DISTANCE) {
            return null
        }

        if (!car.hasWheelContact && car.velocity.y * GoalUtil.getEnemyGoal(car.team).center.y > 0 &&
                ArenaModel.getDistanceFromWall(car.position) > 10 &&
                ArenaModel.getDistanceFromCeiling(car.position) > 15) {
            return startPlan(Plan().withStep(BlindSequence()
                    .withStep(BlindStep(Duration.ofMillis(300), AgentOutput().withPitch(1.0)))
                    .withStep(BlindStep(Duration.ofMillis(100), AgentOutput().withPitch(-1.0).withJump()))
                    .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withUseItem()))), bundle)
        }

        val ceiling = ArenaModel.getCollisionPlanes().first { it.normal.z == -1.0 }
        val carToGoal = GoalUtil.getEnemyGoal(car.team).center - car.position

        if (ArenaModel.isCarOnWall(car) || car.position.z > 20) {
            val targetPosition = car.position.shadowOntoPlane(ceiling) + carToGoal.flatten().scaled(0.5).withZ(ceiling.position.z)
            return SteerUtil.steerTowardPositionAcrossSeam(car, targetPosition)
        }

        val sideWalls = getViableWallPlanes(car.team)
        val targetWall = ArenaModel.getNearestPlane(car.position, sideWalls)
        val targetPosition = car.position.shadowOntoPlane(targetWall) + carToGoal.scaled(0.5).shadowOntoPlane(targetWall).withZ(ceiling.position.z)

        return SteerUtil.steerTowardPositionAcrossSeam(car, targetPosition)
    }

    override fun getLocalSituation(): String {
        return "Carrying spiked ball!"
    }

    companion object {
        fun getViableWallPlanes(team: Team): List<Plane> {
            return ArenaModel.getWallPlanes().filter { Math.abs(it.normal.x) > 0 && !team.opposite().ownsPosition(it.position)   }
        }
    }
}
