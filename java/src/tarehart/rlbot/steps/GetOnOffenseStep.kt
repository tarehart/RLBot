package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.travel.SlideToPositionStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.awt.*
import java.awt.geom.Line2D
import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

class GetOnOffenseStep : Step {
    private var plan: Plan? = null
    private var originalTarget: Vector3? = null
    private var latestTarget: Vector3? = null

    override val situation: String
        get() = "Getting on offense"

    override fun getOutput(input: AgentInput): Optional<AgentOutput> {

        val tacticalSituationOption = TacticsTelemetry.get(input.playerIndex)

        val ballPath = ArenaModel.predictBallPath(input)

        val ballFuture = tacticalSituationOption
                .map { situation -> situation.expectedContact.map { it.space } }
                .orElse(ballPath.getMotionAt(input.time.plusSeconds(4.0)).map { it.space })
                .orElse(input.ballPosition)

        val car = input.myCarData

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

        latestTarget = target
        if (originalTarget == null) {
            originalTarget = target
        }

        val canInterruptPlan = plan?.canInterrupt() != false

        if ((TacticsAdvisor.getYAxisWrongSidedness(car, ballFuture) < -backoff * .6 ||
                originalTarget?.distance(target)?.let { it > 10 } != false ||
                !ArenaModel.isInBoundsBall(target)) && canInterruptPlan) {
            return Optional.empty()
        }

        if (plan != null && !plan!!.isComplete()) {
            val output = plan!!.getOutput(input)
            if (output.isPresent) {
                return output
            }
        }

        val sensibleFlip = SteerUtil.getSensibleFlip(car, target)
        if (sensibleFlip.isPresent) {
            println("Front flip toward offense", input.playerIndex)
            plan = sensibleFlip.get()
            return plan!!.getOutput(input)
        }

        plan = Plan().withStep(SlideToPositionStep { `in` -> Optional.of(PositionFacing(target.flatten(), facing)) })
        return plan!!.getOutput(input)
    }

    override fun canInterrupt(): Boolean {
        return plan == null || plan!!.canInterrupt()
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        if (Plan.activePlan(plan).isPresent) {
            plan!!.currentStep.drawDebugInfo(graphics)
        }

        if (latestTarget != null) {
            graphics.color = Color(190, 61, 66)
            graphics.stroke = BasicStroke(1f)
            val (x, y) = latestTarget!!.flatten()
            val crossSize = 2
            graphics.draw(Line2D.Double(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
            graphics.draw(Line2D.Double(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
        }
    }
}
