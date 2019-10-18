package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.steps.UnfailingStep
import tarehart.rlbot.steps.travel.ParkTheCarStep
import java.awt.Color

class GetOnDefenseStep : NestedPlanStep(), UnfailingStep {
    override fun getLocalSituation(): String {
        return "Getting on defense"
    }


    override fun getUnfailingOutput(bundle: TacticalBundle): AgentOutput {
        // Currently super.getOutput will never return null, so the else condition won't be hit.
        return super.getOutput(bundle) ?: AgentOutput()
    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

//        val threatPosition = bundle.tacticalSituation.expectedEnemyContact?.space ?:
//        bundle.tacticalSituation.futureBallMotion?.space ?:
//        bundle.agentInput.ballPosition

        val car = bundle.agentInput.myCarData
        val ownGoal = GoalUtil.getOwnGoal(car.team)
        val toCenter = ownGoal.center.scaledToMagnitude(-AWAY_FROM_GOAL)
        val anchorPoint = ownGoal.getNearestEntrance(car.position, CENTER_OFFSET) + toCenter
        val centerToAnchor = anchorPoint - (ownGoal.center + toCenter)
        val distanceToAnchor = car.position.flatten().distance(anchorPoint.flatten())
        val waypoint = anchorPoint + centerToAnchor.scaledToMagnitude(Clamper.clamp(distanceToAnchor * .4 - 5, 0.0, 30.0))

        car.renderer.drawLine3d(Color.CYAN, waypoint.toRlbot(), anchorPoint.toRlbot())

        if (distanceToAnchor > 60) {
            SteerUtil.getSensibleFlip(car, waypoint)?.let { return startPlan(it, bundle) }
        }

        if (distanceToAnchor < 5) {
            val plan = Plan(Posture.DEFENSIVE).withStep(ParkTheCarStep {
                val position = (ownGoal.center + toCenter + centerToAnchor.scaledToMagnitude(0.3)).flatten()
                val facing = centerToAnchor.scaledToMagnitude(-1.0).flatten()
                PositionFacing(position, facing)
            })
            return startPlan(plan, bundle)
        }

        return SteerUtil.steerTowardGroundPosition(car, waypoint.flatten(), detourForBoost = true)
    }

    companion object {
        val CENTER_OFFSET = SoccerGoal.EXTENT * .5
        val AWAY_FROM_GOAL = 3.0

        fun calculatePositionFacing(inp: AgentInput): PositionFacing {
            val center = GoalUtil.getOwnGoal(inp.team).center
            val facing = Vector2(BotMath.nonZeroSignum(inp.myCarData.orientation.noseVector.x), 0.0)

            val targetPosition = Vector2(
                    facing.x * -CENTER_OFFSET,
                    center.y - Math.signum(center.y) * AWAY_FROM_GOAL)

            return PositionFacing(targetPosition, facing)
        }
    }
}
