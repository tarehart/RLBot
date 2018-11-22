package tarehart.rlbot.bots

import tarehart.rlbot.tactics.*

class ReliefBot(team: Team, playerIndex: Int) : TacticalBot(team, playerIndex) {

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
