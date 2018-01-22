package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.Bot
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector2.Companion
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.steps.*
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.defense.RotateAndWaitToClearStep
import tarehart.rlbot.steps.defense.WhatASaveStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.wall.DescendFromWallStep
import tarehart.rlbot.steps.wall.MountWallStep
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.util.Optional

import tarehart.rlbot.planning.Plan.Posture.ESCAPEGOAL
import tarehart.rlbot.planning.Plan.Posture.NEUTRAL
import tarehart.rlbot.planning.Plan.Posture.OFFENSIVE
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.tuning.BotLog.println

class TacticsAdvisor {

    fun makePlan(input: AgentInput, situation: TacticalSituation): Plan {

        if (situation.scoredOnThreat != null) {
            return Plan(Plan.Posture.SAVE).withStep(WhatASaveStep())
        }
        //        if (ArenaModel.isBehindGoalLine(input.getMyCarData().getPosition())) {
        //            return new Plan(ESCAPEGOAL).withStep(new EscapeTheGoalStep());
        //        }
        if (situation.waitToClear) {
            return Plan(Plan.Posture.WAITTOCLEAR).withStep(RotateAndWaitToClearStep())
        }
        if (situation.needsDefensiveClear) {
            return FirstViableStepPlan(Plan.Posture.CLEAR)
                    .withStep(DirectedNoseHitStep(KickAwayFromOwnGoal())) // TODO: make these fail if you have to drive through a goal post
                    .withStep(DirectedSideHitStep(KickAwayFromOwnGoal()))
                    .withStep(EscapeTheGoalStep())
                    .withStep(GetOnDefenseStep())
        }
        if (situation.forceDefensivePosture) {
            val secondsToOverrideFor = 0.25
            return Plan(Plan.Posture.DEFENSIVE).withStep(GetOnDefenseStep(secondsToOverrideFor))
        }

        val ownGoalCenter = GoalUtil.getOwnGoal(input.team).center
        val interceptPosition = situation.expectedContact?.space ?: input.ballPosition
        val toOwnGoal = ownGoalCenter.minus(interceptPosition)
        val interceptModifier = toOwnGoal.normaliseCopy()

        if (situation.shotOnGoalAvailable) {

            return FirstViableStepPlan(Plan.Posture.OFFENSIVE)
                    .withStep(DirectedNoseHitStep(KickAtEnemyGoal()))
                    .withStep(DirectedSideHitStep(KickAtEnemyGoal()))
                    .withStep(CatchBallStep())
                    .withStep(GetOnOffenseStep())
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
                makePlanWithPlentyOfTime(input, situation, ballPath)
            }
        }

        // The enemy is probably going to get there first.
        return if (situation.enemyOffensiveApproachError?.let { it < Math.PI / 3 } == true && situation.distanceBallIsBehindUs > -50) {
            // Enemy can probably shoot on goal, so get on defense
            Plan(Plan.Posture.DEFENSIVE).withStep(GetOnDefenseStep())
        } else {
            // Enemy is just gonna hit it for the sake of hitting it, presumably. Let's try to stay on offense if possible.
            // TODO: make sure we don't own-goal it with this
            Plan(Plan.Posture.OFFENSIVE).withStep(GetOnOffenseStep()).withStep(InterceptStep(Vector3()))
        }

    }

    private fun makePlanWithPlentyOfTime(input: AgentInput, situation: TacticalSituation, ballPath: BallPath): Plan {

        val car = input.myCarData

        if (!generousShotAngle(GoalUtil.getEnemyGoal(car.team), situation.expectedContact, car.playerIndex)) {
            val catchOpportunity = SteerUtil.getCatchOpportunity(car, ballPath, car.boost)
            if (catchOpportunity.isPresent) {
                return Plan(Plan.Posture.OFFENSIVE).withStep(CatchBallStep())
            }
        }

        if (DribbleStep.canDribble(input, false) && input.ballVelocity.magnitude() > 15) {
            println("Beginning dribble", input.playerIndex)
            return Plan(OFFENSIVE).withStep(DribbleStep())
        } else if (WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
            return Plan(Plan.Posture.OFFENSIVE).withStep(MountWallStep()).withStep(WallTouchStep()).withStep(DescendFromWallStep())
        } else if (generousShotAngle(GoalUtil.getEnemyGoal(car.team), situation.expectedContact, car.playerIndex) && DirectedNoseHitStep.canMakeDirectedKick(input, KickAtEnemyGoal())) {
            return FirstViableStepPlan(Plan.Posture.OFFENSIVE)
                    .withStep(DirectedNoseHitStep(KickAtEnemyGoal()))
                    .withStep(DirectedNoseHitStep(FunnelTowardEnemyGoal()))
                    .withStep(GetOnOffenseStep())
        } else if (car.boost < 50) {
            return Plan().withStep(GetBoostStep())
        } else if (getYAxisWrongSidedness(input) > 0) {
            println("Getting behind the ball", input.playerIndex)
            return Plan(NEUTRAL).withStep(GetOnOffenseStep())
        } else {
            return Plan(Plan.Posture.OFFENSIVE).withStep(InterceptStep(Vector3()))
        }
    }

    fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation {

        val enemyIntercept = input.enemyCarData.map { car -> getSoonestIntercept(car, ballPath) }.orElse(null)

        val ourIntercept = getSoonestIntercept(input.myCarData, ballPath)

        val zonePlan = ZoneTelemetry.get(input.team)
        val myCar = input.myCarData
        val opponentCar = input.enemyCarData

        val futureBallMotion = ballPath.getMotionAt(input.time.plusSeconds(LOOKAHEAD_SECONDS)).orElse(ballPath.endpoint)
        val enemyGoalY = GoalUtil.getEnemyGoal(input.team).center.y


        val situation = TacticalSituation(
                expectedContact = ourIntercept,
                expectedEnemyContact = enemyIntercept,
                ballAdvantage = calculateRaceResult(ourIntercept?.time, enemyIntercept?.time),
                ownGoalFutureProximity = VectorUtil.flatDistance(GoalUtil.getOwnGoal(input.team).center, futureBallMotion.space),
                distanceBallIsBehindUs = measureOutOfPosition(input),
                enemyOffensiveApproachError = enemyIntercept?.let { measureEnemyApproachError(input, it.toSpaceTime()) },
                distanceFromEnemyBackWall = Math.abs(enemyGoalY - futureBallMotion.space.y),
                distanceFromEnemyCorner = getDistanceFromEnemyCorner(futureBallMotion, enemyGoalY),
                futureBallMotion = futureBallMotion,
                scoredOnThreat = GoalUtil.predictGoalEvent(GoalUtil.getOwnGoal(input.team), ballPath),
                needsDefensiveClear = GoalUtil.ballLingersInBox(GoalUtil.getOwnGoal(input.team), ballPath),
                shotOnGoalAvailable = getShotOnGoalAvailable(input.team, myCar, opponentCar.orElse(null), input.ballPosition, ourIntercept, ballPath),
                forceDefensivePosture = getForceDefensivePosture(input.team, myCar, opponentCar, input.ballPosition),
                goForKickoff = getGoForKickoff(zonePlan, input.team, input.ballPosition),
                waitToClear = getWaitToClear(zonePlan, input),
                currentPlan = currentPlan
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

    private fun getForceDefensivePosture(team: Bot.Team, myCar: CarData, opponentCar: Optional<CarData>,
                                         ballPosition: Vector3): Boolean {
        return opponentCar.map { c -> ZoneUtil.isEnemyOffensiveBreakaway(team, myCar, c, ballPosition) }.orElse(false)
    }

    // Really only used for avoiding "Disable Goal Reset" own goals
    private fun getGoForKickoff(zonePlan: Optional<ZonePlan>, team: Bot.Team, ballPosition: Vector3): Boolean {
        if (zonePlan.isPresent) {
            if (ballPosition.flatten().magnitudeSquared() == 0.0) {
                return if (team == Bot.Team.BLUE)
                    zonePlan.get().myZone.mainZone == Zone.MainZone.BLUE
                else
                    zonePlan.get().myZone.mainZone == Zone.MainZone.ORANGE
            }
        }

        return false
    }

    // Checks to see if the ball is in the box for a while or if we have a breakaway
    private fun getShotOnGoalAvailable(team: Bot.Team, myCar: CarData, opponentCar: CarData?,
                                       ballPosition: Vector3, soonestIntercept: Intercept?, ballPath: BallPath): Boolean {

        return if (myCar.position.distance(ballPosition) < 80 &&
                GoalUtil.ballLingersInBox(GoalUtil.getEnemyGoal(team), ballPath) &&
                generousShotAngle(GoalUtil.getEnemyGoal(myCar.team), soonestIntercept, myCar.playerIndex)) {
            true
        } else opponentCar?.let { ZoneUtil.isMyOffensiveBreakaway(team, myCar, it, ballPosition) } ?: false

    }

    // Checks to see if the ball is in the corner and if the opponent is closer to it
    private fun getWaitToClear(zonePlan: Optional<ZonePlan>, input: AgentInput): Boolean {
        val myGoalLocation = GoalUtil.getOwnGoal(input.team).center
        val myBallDistance = input.ballPosition.distance(input.myCarData.position)
        val enemyBallDistance = input.enemyCarData.map { c -> input.ballPosition.distance(c.position) }.orElse(java.lang.Double.MAX_VALUE)
        val ballDistanceToGoal = input.ballPosition.distance(myGoalLocation)
        val myDistanceToGoal = input.myCarData.position.distance(myGoalLocation)
        //double enemyDistanceToGoal = input.getEnemyCarData().position.distance(myGoalLocation);

        return if (zonePlan.isPresent
                && (myBallDistance > enemyBallDistance // Enemy is closer
                || myDistanceToGoal > ballDistanceToGoal) // Wrong side of the ball

                && (zonePlan.get().ballZone.subZone == Zone.SubZone.TOPCORNER || zonePlan.get().ballZone.subZone == Zone.SubZone.BOTTOMCORNER)) {

            if (input.team == Bot.Team.BLUE)
                zonePlan.get().ballZone.mainZone == Zone.MainZone.BLUE
            else
                zonePlan.get().ballZone.mainZone == Zone.MainZone.ORANGE
        } else false

    }

    private fun measureEnemyApproachError(input: AgentInput, enemyContact: SpaceTime): Double {

        val enemyCarData = input.enemyCarData
        if (!enemyCarData.isPresent) {
            return 0.0
        }

        val enemyCar = enemyCarData.get()
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
    }

}
