package tarehart.rlbot.tactics

import tarehart.rlbot.dropshot.DropshotTileManager


enum class GameMode {
    SOCCER,
    DROPSHOT,
    HOOPS
}

object GameModeSniffer {

    val dropshotTileManager = DropshotTileManager()

    fun getGameMode(): GameMode {
        if (dropshotTileManager.isDropshotMode()) {
            return GameMode.DROPSHOT
        }

        return GameMode.SOCCER
    }

}
