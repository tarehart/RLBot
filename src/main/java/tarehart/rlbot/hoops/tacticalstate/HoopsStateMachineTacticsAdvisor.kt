package tarehart.rlbot.hoops.tacticalstate

import tarehart.rlbot.AgentInput
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.CarWithIntercept
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.ChaseBallStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.TacticalSituation
import tarehart.rlbot.tactics.TacticsAdvisor
import tarehart.rlbot.tactics.TacticsTelemetry

abstract class HoopsStateMachineTacticsAdvisor : TacticsAdvisor {

    private var currentState : TacticalState = IdleState()

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.HOOPS)
    }

    override fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan? {
        return this.currentState.urgentPlan(input, situation, currentPlan)
    }

    override fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan {
        return this.currentState.newPlan(input, situation)
    }

    override fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation {

        val enemyGoGetter = TacticsAdvisor.getCarWithInitiative(input.getTeamRoster(input.team.opposite()), ballPath)
        val enemyIntercept = enemyGoGetter?.intercept
        val enemyCar = enemyGoGetter?.car

        val ourIntercept = TacticsAdvisor.getSoonestIntercept(input.myCarData, ballPath)

        // TODO: Refactor ZoneTelemetry stuff for game mode equality
        // val zonePlan = ZoneTelemetry[input.playerIndex]

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
                goForKickoff = false, // getGoForKickoff(zonePlan, input.team, input.ballPosition),
                currentPlan = currentPlan,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = TacticsAdvisor.getCarWithInitiative(input.getTeamRoster(input.team), ballPath)
                        ?: CarWithIntercept(input.myCarData, null),
                ballPath = ballPath
        )

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry[situation] = input.playerIndex

        // Get a steady state for the state machine
        var infinityCounter = 5;
        do {
            val oldState = this.currentState
            this.currentState = this.currentState.muse(input, situation)
        } while (this.currentState != oldState && infinityCounter-- > 0)
        if (infinityCounter == 0) {
            println("Warning! Hoops State machine didn't reach a steady state.")
        }


        return situation
    }
}