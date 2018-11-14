package tarehart.rlbot.hoops.tacticalstate

import tarehart.rlbot.AgentInput
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.*
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.wall.DescendFromWallStep
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.TacticalSituation
import tarehart.rlbot.tactics.TacticsAdvisor
import tarehart.rlbot.tactics.TacticsTelemetry
import tarehart.rlbot.time.Duration
import java.awt.Color

abstract class HoopsStateMachineTacticsAdvisor : TacticsAdvisor {

    private var currentState : TacticalState = IdleState()

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.HOOPS)
    }

    override fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan? {

        if (Plan.Posture.KICKOFF.canInterrupt(currentPlan)) {
            val muse = KickoffState().muse(input, situation)
            if (muse is KickoffState) {
                return muse.newPlan(input, situation)
            }
        }

        val car = input.myCarData

        if (!car.hasWheelContact && Plan.Posture.LANDING.canInterrupt(currentPlan) && car.position.z > 5) {
            return Plan(Plan.Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }

        GoalUtil.getEnemyGoal(input.team).predictGoalEvent(situation.ballPath)?.let {
            RenderUtil.drawSquare(car.renderer, GoalUtil.getEnemyGoal(input.team).scorePlane, HoopsGoal.RADIUS, Color.PINK)
            return FirstViableStepPlan(Plan.Posture.NEUTRAL)
                    .withStep(DemolishEnemyStep())
                    .withStep(GetBoostStep())
                    .withStep(GetOnDefenseStep())
        }

        if (situation.scoredOnThreat != null && Plan.Posture.SAVE.canInterrupt(currentPlan)) {
            return Plan(Plan.Posture.SAVE).withStep(InterceptStep(Vector3(z = -1.0)))
        }

        return null
    }

    override fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan {

        if (WallTouchStep.hasWallTouchOpportunity(input, situation.ballPath)) {
            return FirstViableStepPlan(Plan.Posture.NEUTRAL)
                    .withStep(WallTouchStep())
                    .withStep(MidairStrikeStep(Duration.ofSeconds(0.3)))
        }

        if (ArenaModel.isCarNearWall(input.myCarData)) {
            return Plan().withStep(DescendFromWallStep())
        }

        return FirstViableStepPlan(Plan.Posture.NEUTRAL)
                .withStep(FlexibleKickStep(WallPass()))
                .withStep(FlexibleKickStep(KickToEnemyHalf()))
                .withStep(GetBoostStep())
                .withStep(GetOnDefenseStep())
    }

    override fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation {

        val teamIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team), ballPath)
        val enemyIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team.opposite()), ballPath)

        val enemyGoGetter = enemyIntercepts.firstOrNull()
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
                enemyOffensiveApproachError = enemyIntercept?.let { TacticsAdvisor.measureApproachError(enemyCar!!, it.space.flatten()) },
                futureBallMotion = futureBallMotion,
                scoredOnThreat = GoalUtil.getOwnGoal(input.team).predictGoalEvent(ballPath),
                needsDefensiveClear = false,
                shotOnGoalAvailable = true,
                goForKickoff = false, // getGoForKickoff(zonePlan, input.team, input.ballPosition),
                currentPlan = currentPlan,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = teamIntercepts.first(),
                teamPlayerWithBestShot = TacticsAdvisor.getCarWithBestShot(teamIntercepts),
                ballPath = ballPath,
                gameMode = GameMode.HOOPS
        )

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry[situation] = input.playerIndex

        // Get a steady state for the state machine
//        var infinityCounter = 5
//        do {
//            val oldState = this.currentState
//            this.currentState = this.currentState.muse(input, situation)
//        } while (this.currentState != oldState && infinityCounter-- > 0)
//        if (infinityCounter == 0) {
//            println("Warning! Hoops State machine didn't reach a steady state.")
//        }

        return situation
    }
}
