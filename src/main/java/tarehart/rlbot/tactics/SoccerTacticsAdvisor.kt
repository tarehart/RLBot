package tarehart.rlbot.tactics

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
import tarehart.rlbot.planning.Posture.*
import tarehart.rlbot.steps.*
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.defense.RotateAndWaitToClearStep
import tarehart.rlbot.steps.defense.ThreatAssessor
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.teamwork.ShadowThePlayStep
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.tactics.TacticsAdvisor.Companion.getYAxisWrongSidedness
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

// TODO: Respect opponents less by only entering the challenge state if their velocity is toward their
// expected intercept. This will lead to more aggression.

// TODO: Classify when opponents are rotating out and respond with aggression

// TODO: Specific rotate out state which gathers big boost if possible

class SoccerTacticsAdvisor: TacticsAdvisor {

    var goNuts = false
    val kickoffAdvisor = KickoffAdvisor()

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.SOCCER)
    }

    override fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        val car = input.myCarData
        val threatReport = ThreatAssessor.getThreatReport(bundle)
        val teamHasMeCovered = RotationAdvisor.teamHasMeCovered(bundle)

        if (teamHasMeCovered) {
            car.renderer.drawString3d("Team has me covered", Color.GREEN, car.position.toRlbot(), 2, 2)
        }

        if (currentPlan == null) {
            BotLog.println("findMoreUrgentPlan, but plan is actually null!", car.playerIndex)
        }

        val scoreAdvantage = input.blueScore - input.orangeScore * if (car.team == Team.BLUE) 1 else -1
        // goNuts = scoreAdvantage < -1
        kickoffAdvisor.gradeKickoff(bundle)

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff) {
            if (situation.teamPlayerWithInitiative?.car == car) {
                val kickoffAdvice = kickoffAdvisor.giveAdvice(GoForKickoffStep.getKickoffType(bundle), bundle)
                return Plan(Posture.KICKOFF).withStep(GoForKickoffStep(
                        dodgeDistance = kickoffAdvice.dodgeRange,
                        counterAttack = kickoffAdvice.counterAttack))
            }

            if (GoForKickoffStep.getKickoffType(bundle) == GoForKickoffStep.KickoffType.CENTER) {
                val expiryTime = car.time.plusSeconds(3)
                return RetryableViableStepPlan(Posture.DEFENSIVE, GetOnDefenseStep()) { b -> b.agentInput.time < expiryTime }
                        .withStep(GetOnDefenseStep())
            }

            return Plan(Posture.KICKOFF).withStep(GetBoostStep())
        }

        if (Posture.LANDING.canInterrupt(currentPlan) && !car.hasWheelContact &&
                !ArenaModel.isBehindGoalLine(car.position)) {

            if (ArenaModel.isMicroGravity() && situation.distanceBallIsBehindUs < 0) {
                return Plan().withStep(MidairStrikeStep(Duration.ofMillis(0)))
            }

            return Plan(Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
        }

        if (situation.scoredOnThreat != null && Posture.SAVE.canInterrupt(currentPlan)) {
            println("Canceling current plan. Need to go for save!", input.playerIndex)
            return SaveAdvisor.planSave(bundle, situation.scoredOnThreat)
        }

        if (!goNuts && getWaitToClear(bundle, situation.enemyPlayerWithInitiative?.car) && Posture.DEFENSIVE.canInterrupt(currentPlan)) {
            println("Canceling current plan. Ball is in the corner and I need to rotate!", input.playerIndex)
            return Plan(Posture.DEFENSIVE).withStep(RotateAndWaitToClearStep())
        }

        if (situation.needsDefensiveClear && Posture.CLEAR.canInterrupt(currentPlan) && situation.teamPlayerWithInitiative?.car == input.myCarData) {

            if (situation.ballAdvantage.seconds < 0.3 && threatReport.challengeImminent) {
                println("Need to clear, but also need to challenge first!", input.playerIndex)
                return RetryableViableStepPlan(CLEAR, GetOnDefenseStep()) {
                    b -> b.tacticalSituation.needsDefensiveClear
                }.withStep(ChallengeStep()).withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
            }

            println("Canceling current plan. Going for clear!", input.playerIndex)

            situation.expectedContact?.let {
                val carToIntercept = it.space - car.position
                val carApproachVsBallApproach = carToIntercept.flatten().correctionAngle(input.ballVelocity.flatten())

                if (Math.abs(carApproachVsBallApproach) > Math.PI / 2) {
                    return Plan(Posture.CLEAR)
                            .withStep(FlexibleKickStep(KickAtEnemyGoal()))
                            .withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
                            .withStep(InterceptStep(Vector3(0.0, Math.signum(GoalUtil.getOwnGoal(car.team).center.y) * 1.5, 0.0)))
                }
            }

            return RetryableViableStepPlan(Posture.CLEAR, GetOnDefenseStep()) { b -> b.tacticalSituation.needsDefensiveClear }
                    .withStep(FlexibleKickStep(KickAwayFromOwnGoal())) // TODO: make these fail if you have to drive through a goal post
        }

        if (DEFENSIVE.canInterrupt(currentPlan)) {

            if (WallTouchStep.hasWallTouchOpportunity(bundle)) {
                return Plan(DEFENSIVE).withStep(WallTouchStep())
            }

            if (threatReport.challengeImminent) {
                return Plan(DEFENSIVE).withStep(ChallengeStep())
            }

            if (!goNuts && !teamHasMeCovered &&
                    threatReport.looksSerious() &&
                    situation.teamPlayerWithInitiative?.car == input.myCarData) {
                println("Canceling current plan due to threat level: $threatReport", input.playerIndex)
                return FirstViableStepPlan(Posture.DEFENSIVE)
                        .withStep(ChallengeStep())
                        .withStep(GetOnDefenseStep())
                        .withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
            }
        }

        val alone = input.getTeamRoster(input.team).size <= 1

        if (situation.shotOnGoalAvailable && (alone || teamHasMeCovered) &&
                !threatReport.looksSerious() && !threatReport.enemyWinsRace &&
                Posture.OFFENSIVE.canInterrupt(currentPlan)
                && situation.teamPlayerWithBestShot?.car == input.myCarData) {

            println("Canceling current plan. Shot opportunity!", input.playerIndex)

            val plan = FirstViableStepPlan(OFFENSIVE)

            if (DribbleStep.canDribble(bundle, true) && situation.expectedContact != null &&
                    Duration.between(car.time, situation.expectedContact.time).seconds < 1.0) {
                plan.withStep(DribbleStep())
            }

            plan.withStep(FlexibleKickStep(KickAtEnemyGoal()))
                    .withStep(CatchBallStep())
                    .withStep(FlexibleKickStep(WallPass()))

            return plan
        }

        return null
    }

    override fun makeFreshPlan(bundle: TacticalBundle): Plan {

        val situation = bundle.tacticalSituation
        val teamHasMeCovered = RotationAdvisor.teamHasMeCovered(bundle)

        val raceResult = situation.ballAdvantage
        val threatReport = ThreatAssessor.getThreatReport(bundle)

        if (threatReport.challengeImminent) {
            return Plan(DEFENSIVE).withStep(ChallengeStep())
        }

        if (WallTouchStep.hasWallTouchOpportunity(bundle)) {
            return Plan(OFFENSIVE).withStep(WallTouchStep())
        }

        if (teamHasMeCovered) {
            if (raceResult.seconds > 0 || !threatReport.enemyShotAligned || goNuts) {
                // We can take our sweet time. Now figure out whether we want a directed kick, a dribble, an intercept, a catch, etc
                return makePlanWithPlentyOfTime(bundle)
            }
        }

        // The enemy is probably going to get there first.
        if (situation.enemyOffensiveApproachError?.let { it < Math.PI / 3 } == true && situation.distanceBallIsBehindUs > -50) {
            // Enemy can probably shoot on goal, so get on defense
            return Plan(Posture.DEFENSIVE).withStep(GetOnDefenseStep())
        } else {

            val canTouchFirst = situation.teamPlayerWithInitiative?.car == bundle.agentInput.myCarData && situation.ballAdvantage.seconds > 0.5

            if (canTouchFirst) {
                return FirstViableStepPlan(OFFENSIVE)
                        .withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
            }

            // Enemy is just gonna hit it for the sake of hitting it, presumably. Let's try to stay on offense if possible.
            return RetryableViableStepPlan(NEUTRAL, ShadowThePlayStep()) { b -> !RotationAdvisor.teamHasMeCovered(b) && !canTouchFirst }
                    .withStep(GetBoostStep())
                    .withStep(GetOnOffenseStep())
        }

    }

    private fun makePlanWithPlentyOfTime(bundle: TacticalBundle): Plan {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        val ballPath = situation.ballPath
        val car = input.myCarData

        if (car.boost < 10) {
            return Plan().withStep(GetBoostStep())
        }

        if (DribbleStep.reallyWantsToDribble(bundle)) {
            return Plan(NEUTRAL).withStep(DribbleStep())
        }

        val enemyGoal = GoalUtil.getEnemyGoal(car.team)

        if (situation.expectedContact != null && generousShotAngle(enemyGoal, situation.expectedContact)) {
            val plan = FirstViableStepPlan(OFFENSIVE)

            if (situation.enemyPlayerWithInitiative == null ||
                    situation.expectedContact.space.z > 5 ||
                    // Check here to see if the enemy is in an excellent defensive position. If so, don't go for that shot
                    // because you're likely to get lobbed.
                    Vector2.alignment(enemyGoal.center.flatten().scaledToMagnitude(5.0),
                            situation.enemyPlayerWithInitiative.car.position.flatten(),
                            situation.expectedContact.space.flatten()) < 0.5) {
                plan.withStep(FlexibleKickStep(KickAtEnemyGoal()))
            } else {
                BotLog.println("Passing up a shot :(", car.playerIndex)
            }

            plan.withStep(FlexibleKickStep(WallPass()))
                    .withStep(GetOnOffenseStep())
                    .withStep(DribbleStep())
                    .withStep(CatchBallStep())

            return plan
        }

        if (car.boost < 50) {
            return Plan().withStep(GetBoostStep())
        }

        if (getYAxisWrongSidedness(input) > 0) {
            println("Getting behind the ball", input.playerIndex)
            return Plan(NEUTRAL).withStep(GetOnOffenseStep())
        }

        if (SteerUtil.getCatchOpportunity(car, ballPath, car.boost) != null) {
            val ownGoal = GoalUtil.getOwnGoal(car.team).center
            BotLog.println("Catch opportunity here", car.playerIndex)
            return FirstViableStepPlan(NEUTRAL)
                    .withStep(CatchBallStep())
                    .withStep(DribbleStep())
        }

        val plan = FirstViableStepPlan(NEUTRAL)
                .withStep(FlexibleKickStep(WallPass()))

        if (car.boost > 80 && situation.enemyPlayerWithInitiative != null &&
                car.position.distance(situation.enemyPlayerWithInitiative.car.position) < 80) {
            plan.withStep(DemolishEnemyStep())
        }
        plan.withStep(GetBoostStep())
        return plan
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
                goForKickoff = getGoForKickoff(myCar, input.ballPosition),
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

    // Checks to see if the ball is in the box for a while or if we have a breakaway
    private fun getShotOnGoalAvailable(team: Team, car: CarData, opponentCar: CarData?,
                                       ballPosition: Vector3, soonestIntercept: Intercept?, ballPath: BallPath): Boolean {

        if (!ManeuverMath.isOnGround(car)) {
            return false
        }

        soonestIntercept?.let {
            if (ArenaModel.SIDE_WALL - Math.abs(it.space.x) < 10) {
                return false
            }
        }

        val enemyGoal = GoalUtil.getEnemyGoal(team)
        val ballEntrance = soonestIntercept?.space?.let { enemyGoal.getNearestEntrance(it, 2.0) } ?: enemyGoal.center
        val shotAlignment = Vector2.alignment(car.position.flatten(), ballPosition.flatten(), ballEntrance.flatten())
        return generousShotAngle(GoalUtil.getEnemyGoal(car.team), soonestIntercept) && shotAlignment > 0
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

        fun generousShotAngle(goal: Goal, expectedContact: Vector2): Boolean {

            val goalCenter = goal.center.flatten()
            val ballToGoal = goalCenter.minus(expectedContact)
            val generousAngle = Vector2.angle(goalCenter, ballToGoal) < Math.PI * .35
            val generousTriangle = measureShotTriangle(goal, expectedContact) > Math.PI / 4

            return generousAngle || generousTriangle
        }

        private fun generousShotAngle(goal: Goal, expectedContact: Intercept?): Boolean {
            return expectedContact?.let { generousShotAngle(goal, it.space.flatten()) } ?: false
        }

        private fun measureShotTriangle(goal: Goal, position: Vector2): Double {

            val rightPost = GoalUtil.transformNearPost(goal.rightPost.flatten(), position)
            val leftPost = GoalUtil.transformNearPost(goal.leftPost.flatten(), position)

            val toRightPost = rightPost.minus(position)
            val toLeftPost = leftPost.minus(position)

            return Vector2.angle(toLeftPost, toRightPost)
        }

        fun getGoForKickoff(car: CarData, ballPosition: Vector3): Boolean {
            return ballPosition.flatten().magnitudeSquared() == 0.0 &&
                    car.team.ownsPosition(car.position)
        }
    }

}
