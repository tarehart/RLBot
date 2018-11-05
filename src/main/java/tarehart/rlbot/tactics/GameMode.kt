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

    fun getGameMode(): GameMode {
        if (dropshotTileManager.isDropshotMode()) {
            return GameMode.DROPSHOT
        }

        if (hoopsManager.isHoopsMode()) {
            return GameMode.HOOPS
        }

        return GameMode.SOCCER
    }

}
