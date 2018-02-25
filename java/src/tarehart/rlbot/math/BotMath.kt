package tarehart.rlbot.math


object BotMath {
    fun nonZeroSignum(value: Double) : Int {
        return if (value < 0) -1 else 1
    }
}