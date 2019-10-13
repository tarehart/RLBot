package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.tactics.SoccerTacticsAdvisor
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class GetOnDefenseStep @JvmOverloads constructor(private val lifespan: Double = DEFAULT_LIFESPAN // seconds
) : NestedPlanStep() {

    override fun getLocalSituation(): String {
        return "Getting on defense"
    }

    private var startTime: GameTime? = null

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {
        if (startTime == null) {
            startTime = bundle.agentInput.time
        }

        val plan = Plan(Plan.Posture.DEFENSIVE).withStep(ParkTheCarStep { inp ->
            calculatePositionFacing(inp)
        })

        return startPlan(plan, bundle)
    }

    override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {
        val st = startTime ?: bundle.agentInput.time
        if (Duration.between(st, bundle.agentInput.time).seconds > lifespan) {
            if (!ThreatAssessor.getThreatReport(bundle).looksSerious()) {
                return true
            }
        }
        return false
    }

    companion object {
        val CENTER_OFFSET = SoccerGoal.EXTENT * .5
        val AWAY_FROM_GOAL = 3.0
        val DEFAULT_LIFESPAN = 1.0

        fun calculatePositionFacing(inp: AgentInput): PositionFacing {
            val center = GoalUtil.getOwnGoal(inp.team).center
            val futureBallPosition = TacticsTelemetry[inp.playerIndex]?.futureBallMotion?.space ?: inp.ballPosition

            val targetPosition = Vector2(
                    -Math.signum(futureBallPosition.x) * CENTER_OFFSET,
                    center.y - Math.signum(center.y) * AWAY_FROM_GOAL)

            val targetFacing = Vector2(BotMath.nonZeroSignum(futureBallPosition.x).toDouble(), 0.0)
            return PositionFacing(targetPosition, targetFacing)
        }
    }
}
