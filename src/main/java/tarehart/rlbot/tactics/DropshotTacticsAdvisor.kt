package tarehart.rlbot.tactics

import rlbot.cppinterop.RLBotDll
import rlbot.flat.QuickChatSelection
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
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.tuning.ManeuverMath

class DropshotTacticsAdvisor: TacticsAdvisor {

    private val threatAssessor = ThreatAssessor()

    override fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan? {
        return null
    }

    override fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan {
        return Plan().withStep(ChaseBallStep())
    }


    override fun assessSituation(input: AgentInput, ballPath: BallPath, currentPlan: Plan?): TacticalSituation {

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
                teamPlayerWithInitiative = getCarWithInitiative(input.getTeamRoster(input.team), ballPath)
                        ?: CarWithIntercept(input.myCarData, null),
                ballPath = ballPath
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
