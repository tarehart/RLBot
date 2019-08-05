package tarehart.rlbot.tuning

import tarehart.rlbot.time.GameTime

import java.util.HashMap

object BotLog {

    private val logMap = HashMap<Int, StringBuilder>()
    private var timeStamp = ""

    fun println(message: String, playerIndex: Int) {

        //getLog(playerIndex).append(timeStamp + message).append("\n")
        println("$timeStamp $playerIndex $message")
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
