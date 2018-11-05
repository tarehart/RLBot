package tarehart.rlbot.bots

import rlbot.cppinterop.RLBotDll
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.*

class ReliefBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private var tacticsAdvisor: TacticsAdvisor? = null

    override fun getOutput(input: AgentInput): AgentOutput {

        val gameMode = GameModeSniffer.getGameMode()

        if (tacticsAdvisor == null || !tacticsAdvisor!!.suitableGameModes().contains(gameMode)) {
            if (gameMode == GameMode.SOCCER) {
                tacticsAdvisor = SoccerTacticsAdvisor()
                println("Game Mode: Soccar")
            } else if (gameMode == GameMode.DROPSHOT) {
                println("Game Mode: Dropshot")
                tacticsAdvisor = DropshotTacticsAdvisor()
            } else if (gameMode == GameMode.HOOPS) {
                println("Game Mode: Hoops")
                tacticsAdvisor = HoopsTacticsAdvisor()
            }
        }

        val car = input.myCarData
        val ballPath = ArenaModel.predictBallPath(input)
        val situation = tacticsAdvisor!!.assessSituation(input, ballPath, currentPlan)

        tacticsAdvisor!!.findMoreUrgentPlan(input, situation, currentPlan)?.let {
            currentPlan = it
        }

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = tacticsAdvisor!!.makeFreshPlan(input, situation)
            //currentPlan = Plan().withStep(FlexibleKickStep(KickAtEnemyGoal()))
        }

        currentPlan?.let {
            if (it.isComplete()) {
                currentPlan = null
            } else {
                it.getOutput(input)?.let { return it }
            }
        }

        return SteerUtil.steerTowardGroundPositionGreedily(car, input.ballPosition.flatten()).withBoost(false)
    }
}
