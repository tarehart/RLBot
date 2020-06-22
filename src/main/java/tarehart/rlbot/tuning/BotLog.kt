package tarehart.rlbot.tuning

import tarehart.rlbot.time.GameTime
import tarehart.rlbot.ui.DisplayFlags
import java.util.*

object BotLog {

    private val logMap = HashMap<Int, StringBuilder>()
    private var timeStamp = ""

    fun println(message: String, playerIndex: Int) {

        //getLog(playerIndex).append(timeStamp + message).append("\n")
        if (DisplayFlags[DisplayFlags.BOT_LOG_IN_CONSOLE] == 1) {
            println("$timeStamp $playerIndex $message")
        }
    }

    private fun getLog(playerIndex: Int): StringBuilder {
        return logMap.getOrPut(playerIndex) { StringBuilder() }
    }

    fun setTimeStamp(time: GameTime) {
        timeStamp = "%03d".format(time.toMillis() % 1000)
    }

//    fun collect(playerIndex: Int): String {
//        val log = getLog(playerIndex)
//        val contents = log.toString()
//        log.setLength(0)
//        return contents
//    }
}
