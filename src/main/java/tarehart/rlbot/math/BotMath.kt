package tarehart.rlbot.math

import kotlin.math.abs


object BotMath {

    const val PI = Math.PI.toFloat()

    fun nonZeroSignum(value: Float) : Int {
        return if (value < 0) -1 else 1
    }

    fun numberDistance(first: Float, second: Float): Float {
        return abs(first - second)
    }
}
