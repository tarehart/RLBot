package tarehart.rlbot.bots

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.planning.FirstViableStepPlan
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.steps.*
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.util.*

class CarryBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex) {
    override fun getOutput(bundle: TacticalBundle): AgentOutput {

        val input = bundle.agentInput
        val car = input.myCarData

        findMoreUrgentPlan(bundle, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = makeFreshPlan(bundle)
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(bundle)?.let { return it }
            }
        }

        return AgentOutput()
    }

    var lastSpeedChange = GameTime(0)
    var desiredSpeed = 10.0

    private fun makeFreshPlan(bundle: TacticalBundle): Plan {
        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        if ((input.time - lastSpeedChange).seconds > 3) {
            desiredSpeed = Random().nextDouble() * 25 + 1
            lastSpeedChange = input.time
        }

        val plan = FirstViableStepPlan(Plan.Posture.NEUTRAL)

        if (CarryBotStep.canCarry(bundle, true)) {
            plan.withStep(CarryBotStep())
        } else {
            plan.withStep(BlindStep(duration = Duration.ofMillis(8), output = AgentOutput()))
        }
                // .withStep(BlindStep(duration = Duration.ofSeconds(2.0), output = AgentOutput()))
                // .withStep(CarryStep())
                // .withStep(CatchBallStep())
                // .withStep(GetBoostStep())

        return plan
    }

    private fun findMoreUrgentPlan(bundle: TacticalBundle, currentPlan: Plan?): Plan? {

        val input = bundle.agentInput
        val car = input.myCarData
        val situation = bundle.tacticalSituation

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        /*
        if (Plan.Posture.KICKOFF.canInterrupt(currentPlan) && situation.goForKickoff && situation.teamPlayerWithInitiative?.car == car) {
            return Plan(Plan.Posture.KICKOFF).withStep(GoForKickoffStep())
        }

        if (situation.scoredOnThreat != null && situation.teamPlayerWithInitiative?.car == car && Plan.Posture.SAVE.canInterrupt(currentPlan)) {
            BotLog.println("Canceling current plan. Need to go for save!", input.playerIndex)
            return Plan(Plan.Posture.SAVE).withStep(WhatASaveStep())
        }
        */
        return null
    }
}
