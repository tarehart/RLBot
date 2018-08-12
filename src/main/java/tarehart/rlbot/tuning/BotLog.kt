package tarehart.rlbot.tuning

import tarehart.rlbot.time.GameTime

import java.util.HashMap

object BotLog {

    private val logMap = HashMap<Int, StringBuilder>()
    private var timeStamp = ""

    fun println(message: String, playerIndex: Int) {

        getLog(playerIndex).append(timeStamp + message).append("\n")
        println(message)
    }

    private fun getLog(playerIndex: Int): StringBuilder {
        if (!logMap.containsKey(playerIndex)) {
            logMap[playerIndex] = StringBuilder()
        }
        return logMap[playerIndex]!!
    }

    fun setTimeStamp(time: GameTime) {

        val minutes = "" + time.toMillis() / 60000
        val seconds = String.format("%02d", time.toMillis() / 1000 % 60)
        timeStamp = if (time.toMillis() > 0) "($minutes:$seconds)" else ""
    }

    fun collect(playerIndex: Int): String {
        val log = getLog(playerIndex)
        val contents = log.toString()
        log.setLength(0)
        return contents
    }
}
