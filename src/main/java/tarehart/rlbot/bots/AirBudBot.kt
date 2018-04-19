package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.*
import tarehart.rlbot.steps.EscapeTheGoalStep
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.GetOnOffenseStep
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.defense.GetOnDefenseStep
import tarehart.rlbot.steps.defense.WhatASaveStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.*
import tarehart.rlbot.steps.wall.WallTouchStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog

class AirBudBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private val tacticsAdvisor: TacticsAdvisor = TacticsAdvisor()

    override fun getOutput(input: AgentInput): AgentOutput {

        val car = input.myCarData
        val ballPath = ArenaModel.predictBallPath(input)
        val situation = tacticsAdvisor.assessSituation(input, ballPath, currentPlan)

        findMoreUrgentPlan(input, situation, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = makeFreshPlan(input, situation)
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(input)?.let { return it }
            }
        }

        return SteerUtil.steerTowardGroundPosition(car, input.boostData, input.ballPosition.flatten()).withBoost(false)
    }

    private fun makeFreshPlan(input: AgentInput, situation: TacticalSituation): Plan {
        val car = input.myCarData

        val plan = FirstViableStepPlan(Plan.Posture.NEUTRAL)

        if (!car.hasWheelContact && car.velocity.z > -5 || input.ballPosition.z < car.position.z) {
            plan.withStep(MidairStrikeStep(Duration.ofMillis(0)))
        }

        val enemyGoalProximity = GoalUtil.getEnemyGoal(car.team).center.distance(input.ballPosition)
        if (situation.shotOnGoalAvailable && enemyGoalProximity < 80 && situation.teamPlayerWithInitiative.car == car) {
            plan.withStep(DirectedNoseHitStep(KickAtEnemyGoal()))
            if (enemyGoalProximity < 50) {
                plan.withStep(DirectedSideHitStep(KickAtEnemyGoal()))
            }
        }

        addGenericSteps(plan, input)
        return plan
    }

    private fun addGenericSteps(plan: Plan, input:AgentInput) {
        plan.withStep(WallTouchStep())
        if (input.myCarData.boost < 30) {
            plan.withStep(GetBoostStep())
        }
        if (input.ballPosition.z > 5) {
            plan.withStep(InterceptStep(Vector3()))
        } else {
            plan.withStep(DirectedNoseHitStep(WallPass()))
        }

        plan.withStep(GetOnOffenseStep())
    }

    private fun findMoreUrgentPlan(input: AgentInput, situation: TacticalSituation, currentPlan: Plan?): Plan? {

        val car = input.myCarData

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (Plan.Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff && situation.teamPlayerWithInitiative.car == car) {
            return Plan(Plan.Posture.KICKOFF).withStep(GoForKickoffStep())
        }

        if (Plan.Posture.LANDING.canInterrupt(currentPlan) && !car.hasWheelContact &&
                car.position.z > 5 && car.boost == 0.0 &&
                !ArenaModel.isBehindGoalLine(car.position)) {
            return Plan(Plan.Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }

        if (situation.scoredOnThreat != null && Plan.Posture.SAVE.canInterrupt(currentPlan)) {
            BotLog.println("Canceling current plan. Need to go for save!", input.playerIndex)
            return Plan(Plan.Posture.SAVE).withStep(WhatASaveStep())
        }

        if (situation.needsDefensiveClear && Plan.Posture.CLEAR.canInterrupt(currentPlan) && situation.teamPlayerWithInitiative.car == input.myCarData) {
            BotLog.println("Canceling current plan. Going for clear!", input.playerIndex)
            return FirstViableStepPlan(Plan.Posture.CLEAR)
                    .withStep(DirectedNoseHitStep(KickAwayFromOwnGoal())) // TODO: make these fail if you have to drive through a goal post
                    .withStep(DirectedSideHitStep(KickAwayFromOwnGoal()))
                    .withStep(EscapeTheGoalStep())
                    .withStep(GetOnDefenseStep())
        }

        return null
    }
}
