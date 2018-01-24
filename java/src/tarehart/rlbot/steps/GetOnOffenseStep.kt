package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.travel.ParkTheCarStep

import java.awt.*
import java.awt.geom.Line2D
import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

class GetOnOffenseStep : NestedPlanStep() {
    private var originalTarget: PositionFacing? = null
    private var latestBallFuture: Vector3? = null
    private var latestTarget: PositionFacing? = null

    override fun getLocalSituation(): String {
        return "Getting on offense"
    }

    override fun doInitialComputation(input: AgentInput) {
        val tacticalSituationOption = TacticsTelemetry.get(input.playerIndex)

        val ballPath = ArenaModel.predictBallPath(input)

        val ballFuture = tacticalSituationOption?.expectedContact?.space ?:
                ballPath.getMotionAt(input.time.plusSeconds(4.0)).map { it.space }.orElse(input.ballPosition)

        latestBallFuture = ballFuture

        val enemyGoal = GoalUtil.getEnemyGoal(input.team)
        val ownGoal = GoalUtil.getOwnGoal(input.team)


        val backoff = 15 + ballFuture.z
        val facing: Vector2

        val target: Vector3

        if (Math.abs(ballFuture.x) < ArenaModel.SIDE_WALL * .8) {
            // Get into a strike position, 10 units behind the ball
            val goalToBall = ballFuture.minus(enemyGoal.getNearestEntrance(ballFuture, -10.0))
            val goalToBallNormal = goalToBall.normaliseCopy()
            facing = goalToBallNormal.flatten().scaled(-1.0)
            target = ballFuture.plus(goalToBallNormal.scaled(backoff))

        } else {
            // Get into a backstop position
            val goalToBall = ballFuture.minus(ownGoal.center)
            val goalToBallNormal = goalToBall.normaliseCopy()
            facing = goalToBallNormal.flatten().scaled(-1.0)
            target = ballFuture.minus(goalToBallNormal.scaled(backoff))
        }

        latestTarget = PositionFacing(target.flatten(), facing)
        if (originalTarget == null) {
            originalTarget = latestTarget
        }
    }

    override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {
        val car = input.myCarData
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

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        val target = latestTarget ?: return null

        val sensibleFlip = SteerUtil.getSensibleFlip(car, target.position)
        if (sensibleFlip.isPresent) {
            println("Front flip toward offense", input.playerIndex)
            return startPlan(sensibleFlip.get(), input)
        }

        return startPlan(
                Plan().withStep(ParkTheCarStep({ latestTarget })),
                input)
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        super.drawDebugInfo(graphics)

        latestTarget?.let {
            graphics.color = Color(190, 61, 66)
            graphics.stroke = BasicStroke(1f)
            val (x, y) = it.position
            val crossSize = 2
            graphics.draw(Line2D.Double(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
            graphics.draw(Line2D.Double(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
        }
    }
}
