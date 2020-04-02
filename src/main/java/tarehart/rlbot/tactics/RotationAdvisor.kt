package tarehart.rlbot.tactics

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.planning.CarWithIntercept
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.time.Duration

object RotationAdvisor {

    fun teamHasMeCovered(bundle: TacticalBundle): Boolean {
        val car = bundle.agentInput.myCarData
        val lastBack = lastBack(bundle)
        return lastBack?.car != car
    }

    fun lastBack(bundle: TacticalBundle): CarWithIntercept? {
        return bundle.tacticalSituation.teamIntercepts.minBy { c -> timeToOwnGoal(c)?.seconds ?: 10F }
    }

    fun timeToOwnGoal(carWithIntercept: CarWithIntercept): Duration? {
        val ownGoal = GoalUtil.getOwnGoal(carWithIntercept.car.team)
        return AccelerationModel.getTravelTime(carWithIntercept.car, carWithIntercept.distancePlot, ownGoal.center)
    }
}
