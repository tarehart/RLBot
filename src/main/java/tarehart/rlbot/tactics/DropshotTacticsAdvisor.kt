package tarehart.rlbot.tactics

import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.steps.CatchBallStep
import tarehart.rlbot.steps.GetOnOffenseStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.travel.ParkTheCarStep
import tarehart.rlbot.steps.wall.DescendFromWallStep
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.time.Duration

class DropshotTacticsAdvisor: TacticsAdvisor {

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.DROPSHOT)
    }

    override fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        val car = input.myCarData

        if (!car.hasWheelContact && Posture.LANDING.canInterrupt(currentPlan) && car.position.z > 5) {
            return Plan(Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }

        situation.ballPath.getLanding(input.time)?.let {

            val isBallFriendly = input.latestBallTouch?.team == input.team
            val willLandOnOwnSide = it.space.y * GoalUtil.getOwnGoal(input.team).center.y > 0

            if (willLandOnOwnSide && !isBallFriendly && Posture.SAVE.canInterrupt(currentPlan)) {
                // Need defense!
                return Plan(Posture.SAVE)
                        .withStep(CatchBallStep())
                        .withStep(InterceptStep(Vector3()))
            }

            val isBouncy = BallPhysics.getGroundBounceEnergy(input.ballPosition.z, input.ballVelocity.z) > 100

            if (!willLandOnOwnSide && isBallFriendly && isBouncy && Posture.OFFENSIVE.canInterrupt(currentPlan)) {
                // The ball is about to score or do some damage. Get out of the way!

                if (situation.teamPlayerWithInitiative?.car == input.myCarData && DemolishEnemyStep.hasEnoughBoost(car)) {
                    return Plan(Posture.OFFENSIVE)
                            .withStep(DemolishEnemyStep())
                }

                return Plan(Posture.NEUTRAL).withStep(GetOnOffenseStep())
            }
        }

        return null
    }

    override fun makeFreshPlan(bundle: TacticalBundle): Plan {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation

        if (WallTouchStep.hasWallTouchOpportunity(bundle)) {
            return FirstViableStepPlan(Posture.NEUTRAL)
                    .withStep(WallTouchStep())
                    .withStep(MidairStrikeStep(Duration.ofSeconds(0.3)))
        }

        if (ArenaModel.isCarNearWall(input.myCarData)) {
            return Plan().withStep(DescendFromWallStep())
        }

        if (situation.teamPlayerWithBestShot?.car == input.myCarData || situation.needsDefensiveClear) {
            return FirstViableStepPlan(Posture.NEUTRAL)
                    .withStep(FlexibleKickStep(KickToEnemyHalf()))
                    .withStep(CatchBallStep())
                    .withStep(InterceptStep(Vector3()))
        }


        val rotationPlan = FirstViableStepPlan(Posture.NEUTRAL)
        if (input.myCarData.boost > 50) {
            rotationPlan.withStep(GetOnOffenseStep())
        }
        val ownSide = GoalUtil.getOwnGoal(input.team).center.scaledToMagnitude(50.0).flatten()
        rotationPlan.withStep(ParkTheCarStep { inp ->
            val facing = inp.ballPosition.flatten() - ownSide
            PositionFacing(ownSide, if (facing.isZero) Vector2(1.0, 0.0) else facing)
        })

        return rotationPlan
    }

    override fun assessSituation(input: AgentInput, currentPlan: Plan?): TacticalBundle {

        val ballPath = ArenaModel.predictBallPath(input)

        // DropshotWallKick().getKickDirection(input.myCarData, input.ballPosition)

        val teamIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team), ballPath)
        val enemyIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team.opposite()), ballPath)

        val enemyGoGetter = enemyIntercepts.firstOrNull()
        val enemyIntercept = enemyGoGetter?.intercept
        val enemyCar = enemyGoGetter?.car

        val ourIntercept = teamIntercepts.asSequence().filter { it.car == input.myCarData }.first().intercept

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
                needsDefensiveClear = (ballPath.getMotionAt(input.time.plusSeconds(3.0)) ?: ballPath.endpoint).space.y * GoalUtil.getOwnGoal(input.team).center.y > 0,
                shotOnGoalAvailable = true,
                goForKickoff = SoccerTacticsAdvisor.getGoForKickoff(input.myCarData, input.ballPosition),
                currentPlan = currentPlan,
                teamIntercepts = teamIntercepts,
                enemyIntercepts = enemyIntercepts,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = teamIntercepts.first(),
                teamPlayerWithBestShot = TacticsAdvisor.getCarWithBestShot(teamIntercepts),
                ballPath = ballPath,
                gameMode = GameMode.DROPSHOT
        )

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry[situation] = input.playerIndex

        val teamPlan = TeamPlan(input, situation)
        TeamTelemetry[teamPlan] = input.playerIndex

        return TacticalBundle(input, situation, teamPlan, zonePlan)
    }
}
