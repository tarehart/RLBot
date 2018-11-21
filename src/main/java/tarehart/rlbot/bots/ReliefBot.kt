package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.tactics.*

class ReliefBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex) {

    override fun getNewTacticsAdvisor(tacticsAdvisor: TacticsAdvisor?): TacticsAdvisor? {
        val gameMode = GameModeSniffer.getGameMode()

        if (tacticsAdvisor == null || !tacticsAdvisor.suitableGameModes().contains(gameMode)) {
            if (gameMode == GameMode.SOCCER) {
                println("Game Mode: Soccar")
                return SoccerTacticsAdvisor()
            } else if (gameMode == GameMode.DROPSHOT) {
                println("Game Mode: Dropshot")
                return DropshotTacticsAdvisor()
            } else if (gameMode == GameMode.HOOPS) {
                println("Game Mode: Hoops")
                return HoopsTacticsAdvisor()
            }
        }
        return null
    }

    // TODO: Make a better implementation for this
    /*
    override fun roundInLimbo(bundle: TacticalBundle) {
        // NeverCast here, I want my hoops code to run even when the game hasn't started.
        if (GameModeSniffer.getGameMode() == GameMode.HOOPS) {
            val ballPath = ArenaModel.predictBallPath(input)
            tacticsAdvisor!!.assessSituation(input, ballPath, null)
        }
    }
    */
}
