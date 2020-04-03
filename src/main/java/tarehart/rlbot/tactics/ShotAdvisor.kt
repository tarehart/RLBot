package tarehart.rlbot.tactics

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.Posture
import tarehart.rlbot.steps.DribbleStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.wall.WallTouchStep

object ShotAdvisor {


    fun planShot(bundle: TacticalBundle, roughIntercept: Intercept): Plan {
        val car = bundle.agentInput.myCarData
        val situation = bundle.tacticalSituation

        val enemyGoal = GoalUtil.getEnemyGoal(car.team)
        val goalEntrance = enemyGoal.getNearestEntrance(roughIntercept.space, 2).flatten()
        val shotAlignment = Vector2.alignment(car.position.flatten(), roughIntercept.space.flatten(), goalEntrance)

        val plan = FirstViableStepPlan(Posture.OFFENSIVE)

        plan.withStep(WallTouchStep())

        val canDoSlotKick = roughIntercept.space.z < ArenaModel.BALL_RADIUS * 1.3
        if (shotAlignment > 0.8 || roughIntercept.space.flatten().distance(enemyGoal.center.flatten()) < 50) {
            if (canDoSlotKick) {
                plan.withStep(SlotKickStep(KickAtEnemyGoal()))
            }
            plan.withStep(FlexibleKickStep(KickAtEnemyGoal()))
        }
        if (canDoSlotKick) {
            plan.withStep(SlotKickStep(WallPass()))
        }
        plan.withStep(FlexibleKickStep(WallPass()))

        if (situation.distanceBallIsBehindUs < 0) {
            plan.withStep(InterceptStep())
        }

        plan.withStep(DribbleStep())

        return plan
    }
}
