package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.bots.Team
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
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
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.tuning.ManeuverMath

class TacticsAdvisor {

    private val threatAssessor = ThreatAssessor()

    fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan? {

        val car = input.myCarData
        val zonePlan = ZoneTelemetry.get(input.playerIndex)

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (Plan.Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff) {
            if (situation.teamPlayerWithInitiative.car == car) {
                return Plan(Plan.Posture.KICKOFF).withStep(GoForKickoffStep())
            }
            return Plan(Plan.Posture.KICKOFF).withStep(GetBoostStep())
        }

        if (Plan.Posture.LANDING.canInterrupt(currentPlan) && !car.hasWheelContact &&
                car.position.z > 5 &&
                !ArenaModel.isBehindGoalLine(car.position)) {
            return Plan(Plan.Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }

        if (situation.scoredOnThreat != null && Plan.Posture.SAVE.canInterrupt(currentPlan)) {

            if (situation.ballAdvantage.seconds < 0 && ChallengeStep.threatExists(situation) &&
                    situation.expectedEnemyContact?.time?.isBefore(situation.scoredOnThreat.time) == true) {
                println("Need to save, but also need to challenge first!", input.playerIndex)
                return Plan(Plan.Posture.SAVE).withStep(ChallengeStep())
            }

            println("Canceling current plan. Need to go for save!", input.playerIndex)
            return Plan(Plan.Posture.SAVE).withStep(WhatASaveStep())
        }

        if (situation.waitToClear && Plan.Posture.WAITTOCLEAR.canInterrupt(currentPlan)) {
            println("Canceling current plan. Ball is in the corner and I need to rotate!", input.playerIndex)
            return Plan(Plan.Posture.WAITTOCLEAR).withStep(RotateAndWaitToClearStep())
        }

        if (zonePlan != null && situation.forceDefensivePosture && Plan.Posture.DEFENSIVE.canInterrupt(currentPlan)) {
            println("Canceling current plan. Forcing defensive rotation!", input.playerIndex)
            val secondsToOverrideFor = 0.25
            return Plan(Plan.Posture.DEFENSIVE).withStep(GetOnDefenseStep(secondsToOverrideFor))
        }

        if (situation.needsDefensiveClear && Plan.Posture.CLEAR.canInterrupt(currentPlan) && situation.teamPlayerWithInitiative.car == input.myCarData) {

            if (situation.ballAdvantage.seconds < 0.3 && ChallengeStep.threatExists(situation)) {
                println("Need to clear, but also need to challenge first!", input.playerIndex)
                return Plan(Plan.Posture.CLEAR).withStep(ChallengeStep())
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
                    .withStep(EscapeTheGoalStep())
                    .withStep(GetOnDefenseStep())
        }

        val totalThreat = threatAssessor.measureThreat(input, situation.enemyPlayerWithInitiative) - situation.ballAdvantage.seconds * 2

        if (totalThreat > 3 && Plan.Posture.DEFENSIVE.canInterrupt(currentPlan)) {
            println("Canceling current plan due to threat level.", input.playerIndex)
            return FirstViableStepPlan(Plan.Posture.DEFENSIVE)
                    .withStep(ChallengeStep())
                    .withStep(GetOnDefenseStep())
                    .withStep(FlexibleKickStep(KickAwayFromOwnGoal()))
        }

        if (situation.shotOnGoalAvailable && Plan.Posture.OFFENSIVE.canInterrupt(currentPlan)) {
            println("Canceling current plan. Shot opportunity!", input.playerIndex)
            return FirstViableStepPlan(Plan.Posture.OFFENSIVE)
                    .withStep(FlexibleKickStep(KickAtEnemyGoal()))
                    .withStep(FlexibleKickStep(WallPass()))
                    .withStep(CatchBallStep())
                    .withStep(InterceptStep(Vector3()))
                    .withStep(GetOnOffenseStep())
                    .withStep(GetBoostStep())
        }

        return null
    }

    fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan {

        if (situation.teamPlayerWithInitiative.car != input.myCarData) {

            if (GoalUtil.getNearestGoal(situation.futureBallMotion?.space ?: input.ballPosition).team == input.team) {
                // Do something vaguely defensive
                return Plan(Plan.Posture.NEUTRAL)
                        .withStep(GetBoostStep())
                        .withStep(GetOnDefenseStep())

            } else {

                return FirstViableStepPlan(Plan.Posture.NEUTRAL)
                        .withStep(GetBoostStep())
                        .withStep(GetOnOffenseStep())
                        .withStep(DemolishEnemyStep())
            }
        }

        val ballPath = ArenaModel.predictBallPath(input)

        val raceResult = situation.ballAdvantage

        if (raceResult.seconds > 2) {
            // We can take our sweet time. Now figure out whether we want a directed kick, a dribble, an intercept, a catch, etc
            return makePlanWithPlentyOfTime(input, situation, ballPath)
        }

        if (raceResult.seconds > -.3) {

            if (situation.enemyOffensiveApproachError?.let { it < Math.PI / 2 } == true) {
                // Enemy is threatening us
                // Consider this to be a 50-50. Go hard for the intercept
                return Plan(Plan.Posture.DEFENSIVE).withStep(ChallengeStep())
            } else {
                // Doesn't matter if enemy wins the race, they are out of position.
                return makePlanWithPlentyOfTime(input, situation, ballPath)
            }
        }

        // The enemy is probably going to get there first.
        return if (situation.enemyOffensiveApproachError?.let { it < Math.PI / 3 } == true && situation.distanceBallIsBehindUs > -50) {
            // Enemy can probably shoot on goal, so get on defense
            Plan(Plan.Posture.DEFENSIVE).withStep(GetOnDefenseStep())
        } else {
            // Enemy is just gonna hit it for the sake of hitting it, presumably. Let's try to stay on offense if possible.
            // TODO: make sure we don't own-goal it with this
            Plan(Plan.Posture.OFFENSIVE).withStep(GetOnOffenseStep())
        }

    }

    private fun makePlanWithPlentyOfTime(input: AgentInput, situation: TacticalSituation, ballPath: BallPath): Plan {

        val car = input.myCarData

        if (DribbleStep.canDribble(input, false) && input.ballVelocity.magnitude() > 15) {
            println("Beginning dribble", input.playerIndex)
            return Plan(OFFENSIVE).withStep(DribbleStep())
        }

        if (WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
            return FirstViableStepPlan(Plan.Posture.OFFENSIVE).withStep(WallTouchStep()).withStep(FlexibleKickStep(WallPass()))
        }

        if (generousShotAngle(GoalUtil.getEnemyGoal(car.team), situation.expectedContact, car.playerIndex)) {
            return FirstViableStepPlan(Plan.Posture.OFFENSIVE)
                    .withStep(FlexibleKickStep(KickAtEnemyGoal()))
                    .withStep(FlexibleKickStep(WallPass()))
                    .withStep(GetOnOffenseStep())
        }

        SteerUtil.getCatchOpportunity(car, ballPath, car.boost)?.let {
            return Plan(Plan.Posture.OFFENSIVE).withStep(CatchBallStep())
        }

        if (car.boost < 50) {
            return Plan().withStep(GetBoostStep())
        }

        if (getYAxisWrongSidedness(input) > 0) {
            println("Getting behind the ball", input.playerIndex)
            return Plan(NEUTRAL).withStep(GetOnOffenseStep())
        }

        return FirstViableStepPlan(Plan.Posture.NEUTRAL)
                .withStep(FlexibleKickStep(WallPass()))
                .withStep(DemolishEnemyStep())
    }

    fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation {

        val enemyGoGetter = getCarWithInitiative(input.getTeamRoster(input.team.opposite()), ballPath)
        val enemyIntercept = enemyGoGetter?.intercept
        val enemyCar = enemyGoGetter?.car

        val ourIntercept = getSoonestIntercept(input.myCarData, ballPath)

        val zonePlan = ZoneTelemetry.get(input.playerIndex)
        val myCar = input.myCarData

        val futureBallMotion = ballPath.getMotionAt(input.time.plusSeconds(LOOKAHEAD_SECONDS)) ?: ballPath.endpoint
        val enemyGoalY = GoalUtil.getEnemyGoal(input.team).center.y


        val situation = TacticalSituation(
                expectedContact = ourIntercept,
                expectedEnemyContact = enemyIntercept,
                ballAdvantage = calculateRaceResult(ourIntercept?.time, enemyIntercept?.time),
                ownGoalFutureProximity = VectorUtil.flatDistance(GoalUtil.getOwnGoal(input.team).center, futureBallMotion.space),
                distanceBallIsBehindUs = measureOutOfPosition(input),
                enemyOffensiveApproachError = enemyIntercept?.let { measureEnemyApproachError(input, enemyCar, it.toSpaceTime()) },
                distanceFromEnemyBackWall = Math.abs(enemyGoalY - futureBallMotion.space.y),
                distanceFromEnemyCorner = getDistanceFromEnemyCorner(futureBallMotion, enemyGoalY),
                futureBallMotion = futureBallMotion,
                scoredOnThreat = GoalUtil.predictGoalEvent(GoalUtil.getOwnGoal(input.team), ballPath),
                needsDefensiveClear = GoalUtil.ballLingersInBox(GoalUtil.getOwnGoal(input.team), ballPath),
                shotOnGoalAvailable = getShotOnGoalAvailable(input.team, myCar, enemyCar, input.ballPosition, ourIntercept, ballPath),
                forceDefensivePosture = getForceDefensivePosture(input.team, myCar, enemyCar, input.ballPosition),
                goForKickoff = getGoForKickoff(zonePlan, input.team, input.ballPosition),
                waitToClear = getWaitToClear(zonePlan, input, enemyCar),
                currentPlan = currentPlan,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = getCarWithInitiative(input.getTeamRoster(input.team), ballPath) ?: CarWithIntercept(input.myCarData, null)
        )

        // Store current TacticalSituation in TacticalTelemetry for Readout display
        TacticsTelemetry[situation] = input.playerIndex

        return situation
    }

    private fun getDistanceFromEnemyCorner(futureBallMotion: BallSlice, enemyGoalY: Double): Double {
        val (x, y) = ArenaModel.CORNER_ANGLE_CENTER
        val goalSign = Math.signum(enemyGoalY)

        val corner1 = Vector2(x, y * goalSign)
        val corner2 = Vector2(-x, y * goalSign)

        val ballFutureFlat = futureBallMotion.space.flatten()

        return Math.min(ballFutureFlat.distance(corner1), ballFutureFlat.distance(corner2))
    }

    private fun getForceDefensivePosture(team: Team, myCar: CarData, opponentCar: CarData?,
                                         ballPosition: Vector3): Boolean {
        return opponentCar?.let { ZoneUtil.isEnemyOffensiveBreakaway(team, myCar, it, ballPosition) } ?: false
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

    // Checks to see if the ball is in the corner and if the opponent is closer to it
    private fun getWaitToClear(zonePlan: ZonePlan?, input: AgentInput, enemyCar: CarData?): Boolean {
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

    private fun measureEnemyApproachError(input: AgentInput, enemyCar: CarData?, enemyContact: SpaceTime): Double {

        if (enemyCar == null) {
            return 0.0
        }

        val myGoal = GoalUtil.getOwnGoal(input.team)
        val ballToGoal = myGoal.center.minus(enemyContact.space)

        val carToBall = enemyContact.space.minus(enemyCar.position)

        return Vector2.angle(ballToGoal.flatten(), carToBall.flatten())
    }


    private fun measureOutOfPosition(input: AgentInput): Double {
        val car = input.myCarData
        val myGoal = GoalUtil.getOwnGoal(input.team)
        val ballToGoal = myGoal.center.minus(input.ballPosition)
        val carToBall = input.ballPosition.minus(car.position)
        val wrongSideVector = VectorUtil.project(carToBall, ballToGoal)
        return wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal))
    }

    companion object {

        private val LOOKAHEAD_SECONDS = 2.0
        private val PLAN_HORIZON = Duration.ofSeconds(6.0)

        fun calculateRaceResult(myIntercept: SpaceTime, enemyCar: CarData, ballPath: BallPath): Duration {
            val enemyIntercept = getSoonestIntercept(enemyCar, ballPath)

            return calculateRaceResult(myIntercept.time, enemyIntercept?.time)
        }

        private fun calculateRaceResult(ourContact: GameTime?, enemyContact: GameTime?): Duration {
            if (enemyContact == null) {
                return Duration.ofSeconds(3.0)
            } else if (ourContact == null) {
                return Duration.ofSeconds(-3.0)
            } else {
                return Duration.between(ourContact, enemyContact)
            }
        }

        fun generousShotAngle(goal: Goal, expectedContact: Vector2, playerIndex: Int): Boolean {

            val goalCenter = goal.center.flatten()
            val ballToGoal = goalCenter.minus(expectedContact)
            val generousAngle = Vector2.angle(goalCenter, ballToGoal) < Math.PI / 4
            val generousTriangle = measureShotTriangle(goal, expectedContact, playerIndex) > Math.PI / 12

            return generousAngle || generousTriangle
        }

        private fun generousShotAngle(goal: Goal, expectedContact: Intercept?, playerIndex: Int): Boolean {
            return expectedContact?.let { generousShotAngle(goal, it.space.flatten(), playerIndex) } ?: false
        }

        private fun measureShotTriangle(goal: Goal, position: Vector2, playerIndex: Int): Double {
            val toRightPost = goal.rightPost.flatten().minus(position)
            val toLeftPost = goal.leftPost.flatten().minus(position)

// BotLog.println(String.format("Shot angle: %.2f", angle), playerIndex);

            return Vector2.angle(toLeftPost, toRightPost)
        }


        private fun getSoonestIntercept(car: CarData, ballPath: BallPath): Intercept? {
            val distancePlot = AccelerationModel.simulateAcceleration(car, PLAN_HORIZON, car.boost)
            return InterceptStep.getSoonestIntercept(car, ballPath, distancePlot, Vector3(), { _, _ -> true })
        }

        fun getYAxisWrongSidedness(input: AgentInput): Double {
            val (_, y) = GoalUtil.getOwnGoal(input.team).center
            val playerToBallY = input.ballPosition.y - input.myCarData.position.y
            return playerToBallY * Math.signum(y)
        }

        fun getYAxisWrongSidedness(car: CarData, ball: Vector3): Double {
            val center = GoalUtil.getOwnGoal(car.team).center
            val playerToBallY = ball.y - car.position.y
            return playerToBallY * Math.signum(center.y)
        }



        fun getCarWithInitiative(cars: List<CarData>, ballPath: BallPath): CarWithIntercept? {

            // TODO: this is pretty expensive. Consider optimizing
            val goGetters = cars.map { CarWithIntercept(it, getSoonestIntercept(it, ballPath)) }
            return goGetters.minBy { it.intercept?.time ?: GameTime(Long.MAX_VALUE) }
        }
    }

}
