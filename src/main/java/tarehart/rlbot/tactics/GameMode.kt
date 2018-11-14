package tarehart.rlbot.tactics

import tarehart.rlbot.dropshot.DropshotTileManager
import tarehart.rlbot.hoops.HoopsManager


enum class GameMode {
    SOCCER,
    DROPSHOT,
    HOOPS
}

object GameModeSniffer {

    val dropshotTileManager = DropshotTileManager()
    val hoopsManager = HoopsManager()

    private var gameMode: GameMode? = null

    fun getGameMode(): GameMode {

        gameMode?.let {
            return it
        }

        if (dropshotTileManager.isDropshotMode()) {
            gameMode = GameMode.DROPSHOT
            return GameMode.DROPSHOT
        }

        if (hoopsManager.isHoopsMode()) {
            gameMode = GameMode.HOOPS
            return GameMode.HOOPS
        }

        return GameMode.SOCCER
    }

}
