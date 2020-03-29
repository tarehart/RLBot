package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.tactics.TacticsAdvisor
import tarehart.rlbot.tuning.BotLog.println
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class GetOnOffenseStep : NestedPlanStep() {
    private var originalTarget: PositionFacing? = null
    private var latestBallFuture: Vector3? = null
    private var latestTarget: PositionFacing? = null

    override fun getLocalSituation(): String {
        return "Getting on offense"
    }

    override fun doInitialComputation(bundle: TacticalBundle) {
        val situation = bundle.tacticalSituation
        val ballPath = bundle.tacticalSituation.ballPath

        val ballFuture = situation.expectedContact.intercept?.space ?:
                ballPath.getMotionAt(bundle.agentInput.time.plusSeconds(4.0))?.space ?: bundle.agentInput.ballPosition

        latestBallFuture = ballFuture

        val enemyGoal = GoalUtil.getEnemyGoal(bundle.agentInput.team)
        val ownGoal = GoalUtil.getOwnGoal(bundle.agentInput.team)


        val backoff = 20 + ballFuture.z
        val facing: Vector2

        val target: Vector3

        if (Math.abs(ballFuture.x) < ArenaModel.SIDE_WALL * .8) {
            // Get into a strike position, 10 units behind the ball
            val goalToBall = ballFuture.minus(enemyGoal.getNearestEntrance(ballFuture, -10.0))
            val goalToBallNormal = goalToBall.normaliseCopy()
            facing = goalToBallNormal.flatten().scaled(-1F)
            target = ballFuture.plus(goalToBallNormal.scaled(backoff))

        } else {
            // Get into a backstop position
            val goalToBall = ballFuture.minus(ownGoal.center)
            val goalToBallNormal = goalToBall.normaliseCopy()
            facing = goalToBallNormal.flatten().normalized()
            target = ballFuture.minus(goalToBallNormal.scaled(backoff))
        }

        latestTarget = PositionFacing(target.flatten(), facing)
        if (originalTarget == null) {
            originalTarget = latestTarget
        }
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        val car = bundle.agentInput.myCarData
        val target = latestTarget ?: return true
        val ballFuture = latestBallFuture ?: return true
        val backoff = ballFuture.flatten().distance(target.position)

        if ((TacticsAdvisor.getYAxisWrongSidedness(car, ballFuture) < -backoff * .6 ||
                originalTarget?.position?.distance(target.position)?.let { it > 10 } != false ||
                !ArenaModel.isInBounds(target.position))) {
            return true
        }

        return false
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val target = latestTarget ?: return null

        SteerUtil.getSensibleFlip(car, target.position)?.let {
            println("Front flip toward offense", bundle.agentInput.playerIndex)
            return startPlan(it, bundle)
        }

        return startPlan(
                Plan().withStep(ParkTheCarStep { target }),
                bundle)
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        super.drawDebugInfo(graphics)

        latestTarget?.let {
            graphics.color = Color(190, 61, 66)
            graphics.stroke = BasicStroke(1f)
            val (x, y) = it.position
            val crossSize = 2
            graphics.draw(Line2D.Float(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
            graphics.draw(Line2D.Float(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
        }
    }
}
