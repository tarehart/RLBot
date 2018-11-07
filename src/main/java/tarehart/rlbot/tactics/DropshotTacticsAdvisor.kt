package tarehart.rlbot.tactics

import tarehart.rlbot.AgentInput
import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.*
import tarehart.rlbot.steps.ChaseBallStep

class DropshotTacticsAdvisor: TacticsAdvisor {

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.DROPSHOT)
    }

    override fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan? {
        return null
    }

    override fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan {
        return Plan().withStep(ChaseBallStep())
    }

    override fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation {

        val enemyGoGetter = TacticsAdvisor.getCarWithInitiative(input.getTeamRoster(input.team.opposite()), ballPath)
        val enemyIntercept = enemyGoGetter?.intercept
        val enemyCar = enemyGoGetter?.car

        val ourIntercept = TacticsAdvisor.getSoonestIntercept(input.myCarData, ballPath)

        val zonePlan = ZoneTelemetry[input.playerIndex]

        val futureBallMotion = ballPath.getMotionAt(input.time.plusSeconds(TacticsAdvisor.LOOKAHEAD_SECONDS)) ?: ballPath.endpoint

        val situation = TacticalSituation(
                expectedContact = ourIntercept,
                expectedEnemyContact = enemyIntercept,
                ballAdvantage = TacticsAdvisor.calculateRaceResult(ourIntercept?.time, enemyIntercept?.time),
                ownGoalFutureProximity = VectorUtil.flatDistance(GoalUtil.getOwnGoal(input.team).center, futureBallMotion.space),
                distanceBallIsBehindUs = TacticsAdvisor.measureOutOfPosition(input),
                enemyOffensiveApproachError = enemyIntercept?.let { TacticsAdvisor.measureEnemyApproachError(input, enemyCar, it.toSpaceTime()) },
                futureBallMotion = futureBallMotion,
                scoredOnThreat = GoalUtil.predictGoalEvent(GoalUtil.getOwnGoal(input.team), ballPath),
                needsDefensiveClear = GoalUtil.ballLingersInBox(GoalUtil.getOwnGoal(input.team), ballPath),
                shotOnGoalAvailable = true,
                goForKickoff = getGoForKickoff(zonePlan, input.team, input.ballPosition),
                currentPlan = currentPlan,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = TacticsAdvisor.getCarWithInitiative(input.getTeamRoster(input.team), ballPath)
                        ?: CarWithIntercept(input.myCarData, null),
                ballPath = ballPath
        )

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry[situation] = input.playerIndex

        return situation
    }

    // Really only used for avoiding "Disable Goal Reset" own goals
    private fun getGoForKickoff(zonePlan: ZonePlan?, team: Team, ballPosition: Vector3): Boolean {
        if (zonePlan != null) {
            if (ballPosition.flatten().magnitudeSquared() == 0.0) {
                return if (team == Team.BLUE)
                    zonePlan.myZone.mainZone == Zone.MainZone.BLUE
                else
                    zonePlan.myZone.mainZone == Zone.MainZone.ORANGE
            }
        }

        return false
    }
}
