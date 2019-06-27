package tarehart.rlbot.steps.teamwork

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.cancellation.BallPathDisruptionMeter
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.strikes.FlexibleKickStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.KickAwayFromOwnGoal
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.tactics.TacticalSituation
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class PositionForPassStep: NestedPlanStep() {

    override fun getLocalSituation(): String {
        return  "Positioning to receive pass"
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        return bundle.tacticalSituation.teamPlayerWithInitiative?.car == bundle.agentInput.myCarData
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val situation = bundle.tacticalSituation
        val ballPath = bundle.tacticalSituation.ballPath
        val car = bundle.agentInput.myCarData

        val ballFuture = situation.expectedContact?.space ?:
        ballPath.getMotionAt(bundle.agentInput.time.plusSeconds(3.0))?.space ?: bundle.agentInput.ballPosition

        val enemyGoal = GoalUtil.getEnemyGoal(bundle.agentInput.team)

        val backoff = 50 + ballFuture.z

        val goalToBall = ballFuture.minus(enemyGoal.getNearestEntrance(ballFuture, -10.0))
        var idealPosition = ballFuture.plus(goalToBall.scaledToMagnitude(backoff)).flatten()

        bundle.agentInput.getTeamRoster(car.team).forEach {
            if (it != car) {
                val teammateToMe = car.position - it.position
                idealPosition += teammateToMe.scaledToMagnitude(20.0).flatten()
            }
        }

        idealPosition = ArenaModel.clampPosition(idealPosition, 20.0)

        RenderUtil.drawSphere(car.renderer, idealPosition.withZ(1.0), 0.6, Color.BLACK)
        car.renderer.drawLine3d(Color.BLACK, car.position.toRlbot(), idealPosition.withZ(1.0).toRlbot())

        SteerUtil.getSensibleFlip(car, idealPosition)?.let {
            println("Front flip toward offense", bundle.agentInput.playerIndex)
            return startPlan(it, bundle)
        }

        return SteerUtil.steerTowardGroundPosition(car, idealPosition, conserveBoost = true)
    }
}
