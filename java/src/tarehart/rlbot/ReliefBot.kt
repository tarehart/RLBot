package tarehart.rlbot

import tarehart.rlbot.input.CarData
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.*
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.GoForKickoffStep
import tarehart.rlbot.steps.debug.TagAlongStep
import tarehart.rlbot.steps.demolition.DemolishEnemyStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.time.GameTime

import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

class ReliefBot(team: Bot.Team, playerIndex: Int) : Bot(team, playerIndex) {

    private val tacticsAdvisor: TacticsAdvisor = TacticsAdvisor()

    override fun getOutput(input: AgentInput): AgentOutput {

        val car = input.myCarData

        //
        //        double enemyHeight = input.getEnemyCarData().get().position.z;
        //        if (enemyHeight > .3407) {
        //            if (startTime == null) {
        //                startTime = input.time;
        //            }
        //            BotLog.println(String.format("%.2f %.2f", TimeUtil.toSeconds(Duration.between(startTime, input.time)), enemyHeight), car.playerIndex);
        //        } else {
        //            if (startTime != null) {
        //                BotLog.println(Duration.between(startTime, input.time).toMillis() + "", car.playerIndex);
        //                startTime = null;
        //            }
        //        }
        //
        //        return new AgentOutput();

        val zonePlan = ZoneTelemetry.get(input.team)
        val ballPath = ArenaModel.predictBallPath(input)
        val situation = tacticsAdvisor.assessSituation(input, ballPath, currentPlan)

        //        if (canInterruptPlanFor(Plan.Posture.OVERRIDE)) {
        //            currentPlan = new Plan(Plan.Posture.OVERRIDE).withStep(new TagAlongStep());
        //        }

        // NOTE: Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (noActivePlanWithPosture(Plan.Posture.KICKOFF) && (!zonePlan.isPresent || situation.goForKickoff) && situation.teamPlayerWithInitiative.car == car) {
            currentPlan = Plan(Plan.Posture.KICKOFF).withStep(GoForKickoffStep())
        }

        if (canInterruptPlanFor(Plan.Posture.LANDING) && !car.hasWheelContact &&
                car.position.z > 5 &&
                !ArenaModel.isBehindGoalLine(car.position)) {
            currentPlan = Plan(Plan.Posture.LANDING).withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }

        if (situation.scoredOnThreat != null && canInterruptPlanFor(Plan.Posture.SAVE)) {
            println("Canceling current plan. Need to go for save!", input.playerIndex)
            currentPlan = null
        } else if (zonePlan.isPresent && situation.forceDefensivePosture && canInterruptPlanFor(Plan.Posture.DEFENSIVE)) {
            println("Canceling current plan. Forcing defensive rotation!", input.playerIndex)
            currentPlan = null
        } else if (situation.waitToClear && canInterruptPlanFor(Plan.Posture.WAITTOCLEAR)) {
            println("Canceling current plan. Ball is in the corner and I need to rotate!", input.playerIndex)
            currentPlan = null
        } else if (situation.needsDefensiveClear && canInterruptPlanFor(Plan.Posture.CLEAR)) {
            println("Canceling current plan. Going for clear!", input.playerIndex)
            currentPlan = null
        } else if (situation.shotOnGoalAvailable && canInterruptPlanFor(Plan.Posture.OFFENSIVE)) {
            println("Canceling current plan. Shot opportunity!", input.playerIndex)
            currentPlan = null
        }

        if (!Plan.activePlan(currentPlan).isPresent) {
            currentPlan = tacticsAdvisor.makePlan(input, situation)
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
}
