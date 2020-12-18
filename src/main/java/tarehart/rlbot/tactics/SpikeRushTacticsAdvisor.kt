package tarehart.rlbot.tactics

import rlbot.cppinterop.RLBotDll
import rlbot.flat.QuickChatSelection
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Ray2
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.*
import tarehart.rlbot.planning.Posture.NEUTRAL
import tarehart.rlbot.planning.Posture.OFFENSIVE
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.GetOnOffenseStep
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.blind.BlindSequence
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.challenge.ChallengeStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.defense.ThreatAssessor
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.spikerush.SpikeCarryStep
import tarehart.rlbot.steps.spikerush.SpikedCeilingShotStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.teamwork.ShadowThePlayStep
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println
import tarehart.rlbot.tuning.ManeuverMath

class SpikeRushTacticsAdvisor: TacticsAdvisor {

    override fun suitableGameModes(): Set<GameMode> {
        return setOf(GameMode.SPIKE_RUSH)
    }

    fun predictCarBump(car: CarData, otherCar: CarData): SpaceTime? {
        if (car.position.z > ManeuverMath.BASE_CAR_Z * 2 ||
            otherCar.position.z > ManeuverMath.BASE_CAR_Z * 2 ||
                car.position.distance(otherCar.position) > 30) {
            return null
        }
        val intersection = Ray2.getIntersection(Ray2.fromCar(car), Ray2.fromCar(otherCar)).first ?: return null

        val ourArrival = car.time + Duration.ofSeconds(car.position.flatten().distance(intersection) / car.velocity.magnitude())
        val enemyArrival = car.time + Duration.ofSeconds(otherCar.position.flatten().distance(intersection) / otherCar.velocity.magnitude())

        if (Math.abs((ourArrival - enemyArrival).seconds) < 0.5) {
            return SpaceTime(intersection.withZ(car.position.z), ourArrival)
        }

        return null
    }

    override fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        val car = input.myCarData
        val ballCarrier = getBallCarrier(bundle.agentInput)

        if (ballCarrier == car) {
            if ((bundle.agentInput.ballPosition - car.position).dotProduct(car.orientation.roofVector) < ArenaModel.BALL_RADIUS * 0.5) {
                // Let go of the ball if it's stuck underneath us.
                return Plan(Posture.SPIKE_CARRY).withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withUseItem()))
            }

            if (Posture.SPIKE_CARRY.canInterrupt(currentPlan)) {

                val ballRelative = car.relativePosition(bundle.agentInput.ballPosition).normaliseCopy()

                if (ballRelative.x > .5 &&
                        ArenaModel.getNearestPlane(car.position, SpikedCeilingShotStep.getViableWallPlanes(car.team))
                                .distance(car.position) < 40) {
                    return Plan(Posture.SPIKE_CARRY).withStep(SpikedCeilingShotStep())
                }

                return Plan(Posture.SPIKE_CARRY).withStep(SpikeCarryStep())
            }

            bundle.agentInput.getTeamRoster(car.team.opposite()).forEach {
                val bump = predictCarBump(car, it)
                if (bump != null && (bump.time - car.time).seconds < 0.5) {

                    if (bundle.agentInput.getTeamRoster(car.team).size > 1) {
                        val teammate = bundle.agentInput.getTeamRoster(car.team)
                                .filter { it != car }
                                .sortedBy { it.position.distance(car.position) }
                                .first()

                        if (teammate.hasWheelContact && car.team.opposite().ownsPosition(car.position) &&
                                car.position.distance(teammate.position) < ArenaModel.BACK_WALL) {
                            // If teammate is closer to the enemy goal
                            val passTarget = teammate.position + teammate.velocity * 0.7
                            return throwToTarget(car, passTarget)
                        }
                    }

                    // Try to dodge by double jumping!
                    return Plan(Posture.SPIKE_CARRY).withStep(BlindSequence()
                            .withStep(BlindStep(Duration.ofMillis(200), AgentOutput().withJump()))
                            .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withJump(false)))
                            .withStep(BlindStep(Duration.ofMillis(100), AgentOutput().withJump(true))))
                }
            }
        }

        if (ballCarrier != null && ballCarrier.team != car.team && Posture.SAVE.canInterrupt(currentPlan)) {
            return Plan(Posture.SAVE).withStep(DemolishEnemyStep(specificTarget = ballCarrier, requireSupersonic = false, isSpikeRush = true))
        }

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff) {
            if (situation.teamPlayerWithInitiative?.car == car) {
                return Plan(Posture.KICKOFF).withStep(GoForKickoffStep())
            }

            if (GoForKickoffStep.getKickoffType(car) == GoForKickoffStep.KickoffType.CENTER) {
                return RetryableViableStepPlan(Posture.DEFENSIVE, "Covering goal as second man on kickoff", GetOnDefenseStep())
                { b -> b.tacticalSituation.goForKickoff }
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

        if (situation.scoredOnThreat != null && ballCarrier == null && Posture.SAVE.canInterrupt(currentPlan)) {

            RLBotDll.sendQuickChat(car.playerIndex, false, QuickChatSelection.Reactions_Noooo)
            println("Canceling current plan. Going for intercept save!", input.playerIndex)
            return Plan(Posture.SAVE).withStep(SlotKickStep(KickAbsolutelyAnywhere()))
        }

        if (SoccerTacticsAdvisor.getWaitToClear(bundle, situation.enemyPlayerWithInitiative?.car) && Posture.DEFENSIVE.canInterrupt(currentPlan)) {
            println("Canceling current plan. Ball is in the corner and I need to rotate!", input.playerIndex)
            return Plan(Posture.DEFENSIVE).withStep(GetOnDefenseStep())
        }

        if (getForceDefensivePosture(car, situation.enemyPlayerWithInitiative?.car, input.ballPosition)
                && Posture.DEFENSIVE.canInterrupt(currentPlan)) {

            println("Canceling current plan. Forcing defensive rotation!", input.playerIndex)
            return RetryableViableStepPlan(Posture.DEFENSIVE, "Defensive posture is forced.", GetOnDefenseStep()) {
                b -> getForceDefensivePosture(b.agentInput.myCarData, b.tacticalSituation.enemyPlayerWithInitiative?.car, b.agentInput.ballPosition)
            }
        }

        val threatReport = ThreatAssessor.getThreatReport(bundle)

        if (threatReport.looksSerious() && situation.ballAdvantage.seconds < -0.5 && Posture.DEFENSIVE.canInterrupt(currentPlan)
                && situation.teamPlayerWithInitiative?.car == input.myCarData) {
            println("Canceling current plan due to threat level.", input.playerIndex)
            return FirstViableStepPlan(Posture.DEFENSIVE)
                    .withStep(GetOnDefenseStep())
                    .withStep(SlotKickStep(KickAwayFromOwnGoal()))
        }

        return null
    }

    private fun throwToTarget(car: CarData, passTarget: Vector3): Plan {
        val relativePos = car.relativePosition(passTarget).normaliseCopy()

        return Plan()
                .unstoppable()
                .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput().withJump(true)))
                .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput()))
                .withStep(BlindStep(Duration.ofSeconds(.15),
                        AgentOutput()
                                .withJump(true)
                                .withPitch(-relativePos.x)
                                .withRoll(-relativePos.y)))
                .withStep(BlindStep(Duration.ofMillis(50), AgentOutput().withUseItem()))
                .withStep(LandGracefullyStep(LandGracefullyStep.FACE_MOTION))
    }

    override fun makeFreshPlan(bundle: TacticalBundle): Plan {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation
        if (situation.teamPlayerWithInitiative?.car != input.myCarData &&
                situation.teamPlayerWithBestShot?.car != input.myCarData) {

            return FirstViableStepPlan(NEUTRAL)
                    .withStep(GetBoostStep())
                    .withStep(ShadowThePlayStep())
                    .withStep(GetOnOffenseStep())
                    .withStep(DemolishEnemyStep())
        }

        val raceResult = situation.ballAdvantage

        if (raceResult.seconds > 2) {
            // We can take our sweet time. Now figure out whether we want a directed kick, a dribble, an intercept, a catch, etc
            return makePlanWithPlentyOfTime(bundle)
        }

        return FirstViableStepPlan(Posture.DEFENSIVE)
                .withStep(SlotKickStep(KickAbsolutelyAnywhere()))
    }

    private fun makePlanWithPlentyOfTime(bundle: TacticalBundle): Plan {

        val input = bundle.agentInput
        val car = input.myCarData

        if (WallTouchStep.hasWallTouchOpportunity(bundle)) {
            return FirstViableStepPlan(OFFENSIVE)
                    .withStep(WallTouchStep())
                    .withStep(SlotKickStep(KickAbsolutelyAnywhere()))
        }

        if (car.boost < 50) {
            return Plan().withStep(GetBoostStep())
        }

        return Plan().withStep(SlotKickStep(KickAbsolutelyAnywhere()))
    }

    override fun assessSituation(input: AgentInput, currentPlan: Plan?): TacticalBundle {

        val ballPath = ArenaModel.predictBallPath(input)

        val teamIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team), ballPath)
        val enemyIntercepts = TacticsAdvisor.getCarIntercepts(input.getTeamRoster(input.team.opposite()), ballPath)

        val enemyGoGetter = enemyIntercepts.firstOrNull()
        val enemyIntercept = enemyGoGetter?.intercept
        val enemyCar = enemyGoGetter?.car

        val ourIntercept = teamIntercepts.asSequence().filter { it.car == input.myCarData }.first()

        val zonePlan = ZonePlan(input)

        val futureBallMotion = ballPath.getMotionAt(input.time.plusSeconds(TacticsAdvisor.LOOKAHEAD_SECONDS)) ?: ballPath.endpoint

        val situation = TacticalSituation(
                expectedContact = ourIntercept,
                expectedEnemyContact = enemyIntercept,
                ballAdvantage = TacticsAdvisor.calculateRaceResult(ourIntercept.intercept?.time, enemyIntercept?.time),
                ownGoalFutureProximity = VectorUtil.flatDistance(GoalUtil.getOwnGoal(input.team).center, futureBallMotion.space),
                distanceBallIsBehindUs = TacticsAdvisor.measureOutOfPosition(input),
                enemyOffensiveApproachError = enemyIntercept?.let { TacticsAdvisor.measureApproachError(enemyCar!!, it.space.flatten()) },
                futureBallMotion = futureBallMotion,
                scoredOnThreat = GoalUtil.getOwnGoal(input.team).predictGoalEvent(ballPath),
                needsDefensiveClear = GoalUtil.ballLingersInBox(GoalUtil.getOwnGoal(input.team) as SoccerGoal, ballPath),
                shotOnGoalAvailable = SoccerTacticsAdvisor.generousShotAngle(GoalUtil.getEnemyGoal(input.team), input.myCarData.position.flatten()),
                goForKickoff = SoccerTacticsAdvisor.getGoForKickoff(input.myCarData, input.ballPosition),
                currentPlan = currentPlan,
                teamIntercepts = teamIntercepts,
                enemyIntercepts = enemyIntercepts,
                enemyPlayerWithInitiative = enemyGoGetter,
                teamPlayerWithInitiative = teamIntercepts.first(),
                teamPlayerWithBestShot = TacticsAdvisor.getCarWithBestShot(teamIntercepts),
                ballPath = ballPath,
                gameMode = GameMode.SPIKE_RUSH
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

    companion object {

        const val SPIKED_DISTANCE = 4.0

        fun getBallCarrier(input: AgentInput): CarData? {
            return input.allCars
                    .sortedBy { c -> (c.position - input.ballPosition).magnitudeSquared() }
                    .firstOrNull { c -> (c.position - input.ballPosition).magnitudeSquared() < SPIKED_DISTANCE * SPIKED_DISTANCE }
        }
    }
}
