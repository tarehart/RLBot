package tarehart.rlbot.tactics

import rlbot.cppinterop.RLBotDll
import rlbot.flat.RumbleOption


enum class GameMode {
    SOCCER,
    DROPSHOT,
    HOOPS,
    SPIKE_RUSH,
    HEATSEEKER
}

object GameModeSniffer {

    private var gameMode: GameMode? = null

    fun getGameMode(): GameMode {

        gameMode?.let {
            return it
        }

        val matchSettings = RLBotDll.getMatchSettings()
        val mode = matchSettings.gameMode()

        if (mode == rlbot.flat.GameMode.Dropshot) {
            gameMode = GameMode.DROPSHOT
        } else if (mode == rlbot.flat.GameMode.Hoops) {
            gameMode = GameMode.HOOPS
        } else if (mode == rlbot.flat.GameMode.Heatseeker){
            gameMode = GameMode.HEATSEEKER
        } else {
            gameMode = GameMode.SOCCER
        }

        if (matchSettings.mutatorSettings().rumbleOption() == RumbleOption.Spike_Rush) {
            gameMode = GameMode.SPIKE_RUSH
        }

        return gameMode ?: GameMode.SOCCER
    }

}
