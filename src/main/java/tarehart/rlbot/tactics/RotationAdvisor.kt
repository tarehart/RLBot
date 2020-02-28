package tarehart.rlbot.tactics

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.planning.GoalUtil

object RotationAdvisor {

    fun teamHasMeCovered(bundle: TacticalBundle): Boolean {
        val car = bundle.agentInput.myCarData
        val teamRoster = bundle.agentInput.getTeamRoster(car.team)

        if (teamRoster.any { it != car && isCoveringDefense(it, bundle) }) {
            return true
        }
        return false
    }

    private fun isCoveringDefense(car: CarData, bundle: TacticalBundle): Boolean {

        val me = bundle.agentInput.myCarData

        if (car.position.y * car.team.side < 0) {
            return false  // Car is not on the defensive half of the field
        }

        val ownGoal = GoalUtil.getOwnGoal(car.team)
        val toGoal = (ownGoal.center - car.position).flatten()
        val vel = car.velocity.flatten()
        val velTowardGoal = vel.dotProduct(toGoal.normalized())

        if (velTowardGoal > AccelerationModel.MEDIUM_SPEED * .9) {
            return true
        }

        if (me.position.y * me.team.side > car.position.y * me.team.side) {
            return false // This teammate is in front of us, so we should not consider them to be covering defense.
        }

        val ballToGoal = (ownGoal.center - bundle.agentInput.ballPosition).flatten()
        if (toGoal.magnitude() * 2 < ballToGoal.magnitude()) {
            return true
        }
        return false
    }

    private fun isChasingBall(car: CarData, bundle: TacticalBundle): Boolean {
        val expectedIntercept = bundle.tacticalSituation.teamIntercepts.first { it.car == car }
        val intercept = expectedIntercept.intercept ?: return false
        val interceptDirection = (intercept.space.flatten() - car.position.flatten()).normalized()
        val speedTowardBall = car.velocity.flatten().dotProduct(interceptDirection)
        return speedTowardBall > AccelerationModel.MEDIUM_SPEED * .7
    }

}
