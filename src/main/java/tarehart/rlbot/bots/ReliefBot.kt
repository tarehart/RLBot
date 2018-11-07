package tarehart.rlbot.bots

import rlbot.cppinterop.RLBotDll
import rlbot.manager.BotLoopRenderer
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.*

class ReliefBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private var tacticsAdvisor: TacticsAdvisor? = null

    private fun gameMode() : GameMode {
        val gameMode = GameModeSniffer.getGameMode()

        if (tacticsAdvisor == null || !tacticsAdvisor!!.suitableGameModes().contains(gameMode)) {
            if (gameMode == GameMode.SOCCER) {
                tacticsAdvisor = SoccerTacticsAdvisor()
                println("Game Mode: Soccar")
                ArenaModel.setSoccerWalls()
            } else if (gameMode == GameMode.DROPSHOT) {
                println("Game Mode: Dropshot")
                tacticsAdvisor = DropshotTacticsAdvisor()
                ArenaModel.setDropshotWalls()
            } else if (gameMode == GameMode.HOOPS) {
                println("Game Mode: Hoops")
                tacticsAdvisor = HoopsTacticsAdvisor()
            }
        }
        return gameMode
    }

    override fun getOutput(input: AgentInput): AgentOutput {

        gameMode()

        ArenaModel.renderWalls(BotLoopRenderer.forBotLoop(this))

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

    override fun roundInLimbo(input: AgentInput) {
        // NeverCast here, I want my hoops code to run even when the game hasn't started.
        if (gameMode() == GameMode.HOOPS) {
            val ballPath = ArenaModel.predictBallPath(input)
            tacticsAdvisor!!.assessSituation(input, ballPath, null)
        }
    }
}
