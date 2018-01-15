package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.Step
import tarehart.rlbot.steps.travel.SlideToPositionStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.awt.*
import java.util.Optional

class GetOnDefenseStep @JvmOverloads constructor(private val lifespan: Double = DEFAULT_LIFESPAN // seconds
) : NestedPlanStep() {

    override fun getLocalSituation(): String {
        return "Getting on defense"
    }

    private var startTime: GameTime? = null

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {
        if (startTime == null) {
            startTime = input.time
        }

        val plan = Plan(Plan.Posture.DEFENSIVE).withStep(SlideToPositionStep { inp ->

            val goalPos = GoalUtil.getOwnGoal(inp.team).center
            val futureBallPosition = TacticsTelemetry[inp.playerIndex]?.futureBallMotion?.space ?:
                    inp.ballPosition

            val targetPosition = Vector2(
                    Math.signum(futureBallPosition.x) * CENTER_OFFSET,
                    goalPos.y - Math.signum(goalPos.y) * AWAY_FROM_GOAL)

            val targetFacing = Vector2(-Math.signum(targetPosition.x), 0.0)
            PositionFacing(targetPosition, targetFacing)
        })

        return startPlan(plan, input)
    }

    override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {
        val st = startTime ?: input.time
        return Duration.between(st, input.time).seconds > lifespan
    }

    companion object {
        private val CENTER_OFFSET = Goal.EXTENT * .5
        private val AWAY_FROM_GOAL = 3.0
        private val DEFAULT_LIFESPAN = 1.0
    }
}
