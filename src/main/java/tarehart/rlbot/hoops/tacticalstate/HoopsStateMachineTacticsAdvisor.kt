package tarehart.rlbot.hoops.tacticalstate

import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.math.Ray
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.*
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.travel.AchieveVelocityStep
import tarehart.rlbot.steps.travel.FlyToTargetStep
import tarehart.rlbot.steps.wall.DescendFromWallStep
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.tactics.*
import tarehart.rlbot.time.Duration
import tarehart.rlbot.ui.DisplayFlags
import java.awt.Color

abstract class HoopsStateMachineTacticsAdvisor : TacticsAdvisor {

    private var currentState : TacticalState = IdleState()

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.HOOPS)
    }

    override fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation
        if (Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff) {
            if (situation.teamPlayerWithInitiative?.car == car) {
                return Plan(Posture.KICKOFF).withStep(GoForKickoffStep())
            }

            if (GoForKickoffStep.getKickoffType(bundle) == GoForKickoffStep.KickoffType.CENTER) {
                return RetryableViableStepPlan(Posture.DEFENSIVE, "Second man on center kickoff", GetOnDefenseStep()) { b -> b.tacticalSituation.goForKickoff }
            }

            return Plan(Posture.KICKOFF).withStep(GetBoostStep())
        }

        if (!car.hasWheelContact && Posture.LANDING.canInterrupt(currentPlan) && !ArenaModel.isMicroGravity()) {
            return Plan(Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }

        if (car.hasWheelContact && ArenaModel.isMicroGravity() && Posture.LANDING.canInterrupt(currentPlan)) {
            return Plan(Posture.LANDING)
                    .withStep(AchieveVelocityStep((input.ballPosition - car.position).scaledToMagnitude(10.0)))
//                    .withStep(BlindStep(Duration.ofSeconds(0.05), AgentOutput().withJump()))
//                    .withStep(BlindStep(Duration.ofSeconds(0.05), AgentOutput()))
//                    .withStep(BlindStep(Duration.ofSeconds(0.05), AgentOutput().withJump()))
//                    .withStep(BlindStep(Duration.ofSeconds(0.01), AgentOutput()))
        }

        if (ArenaModel.isMicroGravity()) {
            return null
        }

        GoalUtil.getEnemyGoal(input.team).predictGoalEvent(situation.ballPath)?.let {
            if(DisplayFlags[DisplayFlags.HOOPS_GOAL_PREDICTION] == 1) {
                RenderUtil.drawSquare(car.renderer, GoalUtil.getEnemyGoal(input.team).scorePlane, HoopsGoal.RADIUS, Color.PINK)
            }
            return RetryableViableStepPlan(Posture.NEUTRAL, "We're about to score", GetOnDefenseStep())
                    .withStep(DemolishEnemyStep())
                    .withStep(GetBoostStep())
        }

        if (situation.scoredOnThreat != null && Posture.SAVE.canInterrupt(currentPlan)) {
            return Plan(Posture.SAVE).withStep(InterceptStep(Vector3(z = -1.0)))
        }

        return null
    }

    override fun makeFreshPlan(bundle: TacticalBundle): Plan {
        val input = bundle.agentInput

        val car = input.myCarData

        if (!car.hasWheelContact && ArenaModel.isMicroGravity()) {

            val impact = LandGracefullyStep.predictImpact(bundle)
            if (impact != null && Duration.between(car.time, impact.time).seconds < 1.0) {
                return Plan(Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))

            }

            val goalToBall = input.ballPosition - GoalUtil.getEnemyGoal(car.team).center
            val ballToCar = car.position - input.ballPosition
            if (goalToBall.dotProduct(ballToCar) > 0) {
                return Plan(Posture.OFFENSIVE)
                        .withStep(MidairStrikeStep(Duration.ofMillis(0)))
            }

            return Plan(Posture.OFFENSIVE)
                    .withStep(FlyToTargetStep(
                        ArenaModel.getBounceNormal(Ray(bundle.tacticalSituation.futureBallMotion!!.space, goalToBall)).position))

        }

        if (WallTouchStep.hasWallTouchOpportunity(bundle)) {
            return FirstViableStepPlan(Posture.NEUTRAL)
                    .withStep(WallTouchStep())
                    .withStep(MidairStrikeStep(Duration.ofSeconds(0.3)))
        }

        if (ArenaModel.isCarNearWall(input.myCarData)) {
            return Plan().withStep(DescendFromWallStep())
        }

        return FirstViableStepPlan(Posture.NEUTRAL)
                .withStep(FlexibleKickStep(DunkIntoHoop()))
                .withStep(FlexibleKickStep(WallPass()))
                .withStep(FlexibleKickStep(KickToEnemyHalf()))
                .withStep(GetBoostStep())
                .withStep(GetOnDefenseStep())
    }

    override fun assessSituation(input: AgentInput, currentPlan: Plan?): TacticalBundle {

        val ballPath = ArenaModel.predictBallPath(input)
        val teamIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team), ballPath)
        val enemyIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team.opposite()), ballPath)

        val enemyGoGetter = enemyIntercepts.firstOrNull()
        val enemyIntercept = enemyGoGetter?.intercept
        val enemyCar = enemyGoGetter?.car

        val car = input.myCarData
        val ourIntercept = teamIntercepts.first { it.car.playerIndex == car.playerIndex }.intercept

        val zonePlan = ZonePlan(input)

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
                goForKickoff = SoccerTacticsAdvisor.getGoForKickoff(car, input.ballPosition),
                currentPlan = currentPlan,
                teamIntercepts = teamIntercepts,
                enemyIntercepts = enemyIntercepts,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = teamIntercepts.first(),
                teamPlayerWithBestShot = TacticsAdvisor.getCarWithBestShot(teamIntercepts),
                ballPath = ballPath,
                gameMode = GameMode.HOOPS
        )

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry[situation] = input.playerIndex

        val teamPlan = TeamPlan(input, situation)
        TeamTelemetry[teamPlan] = input.playerIndex

        return TacticalBundle(input, situation, teamPlan, zonePlan)
    }
}
