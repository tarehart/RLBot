package tarehart.rlbot.tactics

import rlbot.cppinterop.RLBotDll
import rlbot.flat.QuickChatSelection
import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.*
import tarehart.rlbot.planning.Plan.Posture.NEUTRAL
import tarehart.rlbot.planning.Plan.Posture.OFFENSIVE
import tarehart.rlbot.steps.*
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.defense.RotateAndWaitToClearStep
import tarehart.rlbot.steps.defense.ThreatAssessor
import tarehart.rlbot.steps.defense.WhatASaveStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.tactics.TacticsAdvisor.Companion.getYAxisWrongSidedness
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.tuning.ManeuverMath

class SoccerTacticsAdvisor: TacticsAdvisor {

    private val threatAssessor = ThreatAssessor()

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.SOCCER)
    }

    override fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        val car = input.myCarData
        val zonePlan = bundle.zonePlan

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (Plan.Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff) {
            if (situation.teamPlayerWithInitiative?.car == car) {
                return Plan(Plan.Posture.KICKOFF).withStep(GoForKickoffStep())
            }
            return Plan(Plan.Posture.KICKOFF).withStep(GetBoostStep())
        }

        if (Plan.Posture.LANDING.canInterrupt(currentPlan) && !car.hasWheelContact &&
                !ArenaModel.isBehindGoalLine(car.position)) {

            if (ArenaModel.isMicroGravity() && situation.distanceBallIsBehindUs < 0) {
                return Plan().withStep(MidairStrikeStep(Duration.ofMillis(0)))
            }

            return Plan(Plan.Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }

        if (situation.scoredOnThreat != null && Plan.Posture.SAVE.canInterrupt(currentPlan)) {

            RLBotDll.sendQuickChat(car.playerIndex, false, QuickChatSelection.Reactions_Noooo)
            if (situation.ballAdvantage.seconds < 0 && ChallengeStep.threatExists(situation) &&
                    situation.expectedEnemyContact?.time?.isBefore(situation.scoredOnThreat.time) == true &&
                    situation.distanceBallIsBehindUs < 0) {
                println("Need to save, but also need to challenge first!", input.playerIndex)
                return Plan(Plan.Posture.SAVE).withStep(ChallengeStep())
            }

            println("Canceling current plan. Need to go for save!", input.playerIndex)
            return Plan(Plan.Posture.SAVE).withStep(WhatASaveStep())
        }

        if (getWaitToClear(bundle, situation.enemyPlayerWithInitiative?.car) && Plan.Posture.WAITTOCLEAR.canInterrupt(currentPlan)) {
            println("Canceling current plan. Ball is in the corner and I need to rotate!", input.playerIndex)
            return Plan(Plan.Posture.WAITTOCLEAR).withStep(RotateAndWaitToClearStep())
        }

        if (getForceDefensivePosture(car, situation.enemyPlayerWithInitiative?.car, input.ballPosition)
                && Plan.Posture.DEFENSIVE.canInterrupt(currentPlan)) {

            println("Canceling current plan. Forcing defensive rotation!", input.playerIndex)
            val secondsToOverrideFor = 0.25
            return Plan(Plan.Posture.DEFENSIVE).withStep(GetOnDefenseStep(secondsToOverrideFor))
        }

        if (situation.needsDefensiveClear && Plan.Posture.CLEAR.canInterrupt(currentPlan) && situation.teamPlayerWithInitiative?.car == input.myCarData) {

            if (situation.ballAdvantage.seconds < 0.3 && ChallengeStep.threatExists(situation)) {
                println("Need to clear, but also need to challenge first!", input.playerIndex)
                return FirstViableStepPlan(Plan.Posture.CLEAR).withStep(ChallengeStep()).withStep(GetOnDefenseStep(0.25))
            }

            println("Canceling current plan. Going for clear!", input.playerIndex)

            situation.expectedContact?.let {
                val carToIntercept = it.space - car.position
                val carApproachVsBallApproach = carToIntercept.flatten().correctionAngle(input.ballVelocity.flatten())

                if (Math.abs(carApproachVsBallApproach) > Math.PI / 2) {
                    return Plan(Plan.Posture.CLEAR).withStep(InterceptStep(Vector3(0.0, Math.signum(GoalUtil.getOwnGoal(car.team).center.y) * 1.5, 0.0)))
                }
            }

            return FirstViableStepPlan(Plan.Posture.CLEAR)
                    .withStep(FlexibleKickStep(KickAwayFromOwnGoal())) // TODO: make these fail if you have to drive through a goal post
                    .withStep(GetOnDefenseStep())
        }

        val totalThreat = threatAssessor.measureThreat(bundle, situation.enemyPlayerWithInitiative)

        if (totalThreat > 3 && situation.ballAdvantage.seconds < 0.5 && Plan.Posture.DEFENSIVE.canInterrupt(currentPlan)
                && situation.teamPlayerWithInitiative?.car == input.myCarData) {
            println("Canceling current plan due to threat level.", input.playerIndex)
            return FirstViableStepPlan(Plan.Posture.DEFENSIVE)
                    .withStep(ChallengeStep())
                    .withStep(GetOnDefenseStep())
                    .withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
        }

        if (situation.shotOnGoalAvailable && Plan.Posture.OFFENSIVE.canInterrupt(currentPlan)
                && situation.teamPlayerWithBestShot?.car == input.myCarData) {

            println("Canceling current plan. Shot opportunity!", input.playerIndex)
            return FirstViableStepPlan(OFFENSIVE)
                    .withStep(FlexibleKickStep(KickAtEnemyGoal()))
                    .withStep(CatchBallStep())
                    .withStep(GetOnOffenseStep())
        }

        return null
    }

    override fun makeFreshPlan(bundle: TacticalBundle): Plan {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        if (situation.teamPlayerWithInitiative?.car != input.myCarData) {

            if (GoalUtil.getNearestGoal(situation.futureBallMotion?.space
                            ?: input.ballPosition).team == input.team) {
                // Do something vaguely defensive
                return Plan(NEUTRAL)
                        .withStep(GetBoostStep())
                        .withStep(GetOnDefenseStep())

            } else {

                return FirstViableStepPlan(NEUTRAL)
                        .withStep(GetBoostStep())
                        .withStep(GetOnOffenseStep())
                        .withStep(DemolishEnemyStep())
            }
        }

        val raceResult = situation.ballAdvantage

        if (raceResult.seconds > 2) {
            // We can take our sweet time. Now figure out whether we want a directed kick, a dribble, an intercept, a catch, etc
            return makePlanWithPlentyOfTime(bundle)
        }

        if (raceResult.seconds > -.3) {

            if (situation.enemyOffensiveApproachError?.let { it < Math.PI / 2 } == true) {
                // Enemy is threatening us
                // Consider this to be a 50-50. Go hard for the intercept
                return Plan(Plan.Posture.DEFENSIVE).withStep(ChallengeStep())
            } else {
                // Doesn't matter if enemy wins the race, they are out of position.
                return makePlanWithPlentyOfTime(bundle)
            }
        }

        // The enemy is probably going to get there first.
        return if (situation.enemyOffensiveApproachError?.let { it < Math.PI / 3 } == true && situation.distanceBallIsBehindUs > -50) {
            // Enemy can probably shoot on goal, so get on defense
            Plan(Plan.Posture.DEFENSIVE).withStep(GetOnDefenseStep())
        } else {
            // Enemy is just gonna hit it for the sake of hitting it, presumably. Let's try to stay on offense if possible.
            // TODO: make sure we don't own-goal it with this
            FirstViableStepPlan(NEUTRAL).withStep(GetOnOffenseStep()).withStep(DribbleStep())
        }

    }

    private fun makePlanWithPlentyOfTime(bundle: TacticalBundle): Plan {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        val ballPath = situation.ballPath
        val car = input.myCarData

        if (WallTouchStep.hasWallTouchOpportunity(bundle)) {
            return FirstViableStepPlan(OFFENSIVE).withStep(WallTouchStep()).withStep(FlexibleKickStep(WallPass()))
        }

        if (DribbleStep.reallyWantsToDribble(bundle)) {
            return Plan(NEUTRAL).withStep(DribbleStep())
        }

        if (generousShotAngle(GoalUtil.getEnemyGoal(car.team), situation.expectedContact, car.playerIndex)) {
            return FirstViableStepPlan(OFFENSIVE)
                    .withStep(FlexibleKickStep(KickAtEnemyGoal()))
                    .withStep(FlexibleKickStep(WallPass()))
                    .withStep(GetOnOffenseStep())
        }

        SteerUtil.getCatchOpportunity(car, ballPath, car.boost)?.let {
            return Plan(NEUTRAL).withStep(CatchBallStep()).withStep(DribbleStep())
        }

        if (car.boost < 50) {
            return Plan().withStep(GetBoostStep())
        }

        if (getYAxisWrongSidedness(input) > 0) {
            println("Getting behind the ball", input.playerIndex)
            return Plan(NEUTRAL).withStep(GetOnOffenseStep())
        }

        return FirstViableStepPlan(NEUTRAL)
                .withStep(FlexibleKickStep(WallPass()))
                .withStep(DemolishEnemyStep())
    }

    override fun assessSituation(input: AgentInput, currentPlan: Plan?): TacticalBundle {

        val ballPath = ArenaModel.predictBallPath(input)

        val teamIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team), ballPath)
        val enemyIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team.opposite()), ballPath)

        val enemyGoGetter = enemyIntercepts.firstOrNull()
        val enemyIntercept = enemyGoGetter?.intercept
        val enemyCar = enemyGoGetter?.car

        val ourIntercept = teamIntercepts.asSequence().filter { it.car == input.myCarData }.first().intercept

        val zonePlan = ZonePlan(input)
        val myCar = input.myCarData

        val futureBallMotion = ballPath.getMotionAt(input.time.plusSeconds(TacticsAdvisor.LOOKAHEAD_SECONDS)) ?: ballPath.endpoint
        val enemyGoalY = GoalUtil.getEnemyGoal(input.team).center.y

        val situation = TacticalSituation(
                expectedContact = ourIntercept,
                expectedEnemyContact = enemyIntercept,
                ballAdvantage = TacticsAdvisor.calculateRaceResult(ourIntercept?.time, enemyIntercept?.time),
                ownGoalFutureProximity = VectorUtil.flatDistance(GoalUtil.getOwnGoal(input.team).center, futureBallMotion.space),
                distanceBallIsBehindUs = TacticsAdvisor.measureOutOfPosition(input),
                enemyOffensiveApproachError = enemyIntercept?.let { TacticsAdvisor.measureApproachError(enemyCar!!, it.space.flatten()) },
                futureBallMotion = futureBallMotion,
                scoredOnThreat = GoalUtil.getOwnGoal(input.team).predictGoalEvent(ballPath),
                needsDefensiveClear = GoalUtil.ballLingersInBox(GoalUtil.getOwnGoal(input.team) as SoccerGoal, ballPath),
                shotOnGoalAvailable = getShotOnGoalAvailable(input.team, myCar, enemyCar, input.ballPosition, ourIntercept, ballPath),
                goForKickoff = getGoForKickoff(zonePlan, input.team, input.ballPosition),
                currentPlan = currentPlan,
                teamIntercepts = teamIntercepts,
                enemyIntercepts = enemyIntercepts,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = teamIntercepts.first(),
                teamPlayerWithBestShot = TacticsAdvisor.getCarWithBestShot(teamIntercepts),
                ballPath = ballPath,
                gameMode = GameMode.SOCCER
        )

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry[situation] = input.playerIndex

        val teamPlan = TeamPlan(input, situation)
        TeamTelemetry[teamPlan] = input.playerIndex

        return TacticalBundle(input, situation, teamPlan, zonePlan)
    }

    private fun getForceDefensivePosture(myCar: CarData, opponentCar: CarData?,
                                         ballPosition: Vector3): Boolean {
        return opponentCar?.let { ZoneUtil.isEnemyOffensiveBreakaway(myCar, it, ballPosition) } ?: false
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

    // Checks to see if the ball is in the box for a while or if we have a breakaway
    private fun getShotOnGoalAvailable(team: Team, myCar: CarData, opponentCar: CarData?,
                                       ballPosition: Vector3, soonestIntercept: Intercept?, ballPath: BallPath): Boolean {

        if (!ManeuverMath.isOnGround(myCar)) {
            return false
        }

        soonestIntercept?.let {
            if (ArenaModel.SIDE_WALL - Math.abs(it.space.x) < 10) {
                return false
            }
        }

        return generousShotAngle(GoalUtil.getEnemyGoal(myCar.team), soonestIntercept, myCar.playerIndex)
    }

    companion object {

        // Checks to see if the ball is in the corner and if the opponent is closer to it
        fun getWaitToClear(bundle: TacticalBundle, enemyCar: CarData?): Boolean {
            val input = bundle.agentInput
            val zonePlan = bundle.zonePlan
            val myGoalLocation = GoalUtil.getOwnGoal(input.team).center
            val myBallDistance = input.ballPosition.distance(input.myCarData.position)
            val enemyBallDistance = enemyCar?.let { c -> input.ballPosition.distance(c.position) } ?: java.lang.Double.MAX_VALUE
            val ballDistanceToGoal = input.ballPosition.distance(myGoalLocation)
            val myDistanceToGoal = input.myCarData.position.distance(myGoalLocation)
            //double enemyDistanceToGoal = input.getEnemyCarData().position.distance(myGoalLocation);

            return if (zonePlan != null
                    && (myBallDistance > enemyBallDistance // Enemy is closer
                            || myDistanceToGoal > ballDistanceToGoal) // Wrong side of the ball

                    && (zonePlan.ballZone.subZone == Zone.SubZone.TOPCORNER || zonePlan.ballZone.subZone == Zone.SubZone.BOTTOMCORNER)) {

                if (input.team == Team.BLUE)
                    zonePlan.ballZone.mainZone == Zone.MainZone.BLUE
                else
                    zonePlan.ballZone.mainZone == Zone.MainZone.ORANGE
            } else false

        }

        fun generousShotAngle(goal: Goal, expectedContact: Vector2, playerIndex: Int): Boolean {

            val goalCenter = goal.center.flatten()
            val ballToGoal = goalCenter.minus(expectedContact)
            val generousAngle = Vector2.angle(goalCenter, ballToGoal) < Math.PI / 4
            val generousTriangle = measureShotTriangle(goal, expectedContact, playerIndex) > Math.PI / 3

            return generousAngle || generousTriangle
        }

        private fun generousShotAngle(goal: Goal, expectedContact: Intercept?, playerIndex: Int): Boolean {
            return expectedContact?.let { generousShotAngle(goal, it.space.flatten(), playerIndex) } ?: false
        }

        private fun measureShotTriangle(goal: Goal, position: Vector2, playerIndex: Int): Double {

            val rightPost = GoalUtil.transformNearPost(goal.rightPost.flatten(), position)
            val leftPost = GoalUtil.transformNearPost(goal.leftPost.flatten(), position)

            val toRightPost = rightPost.minus(position)
            val toLeftPost = leftPost.minus(position)

// BotLog.println(String.format("Shot angle: %.2f", angle), playerIndex);

            return Vector2.angle(toLeftPost, toRightPost)
        }
    }

}
