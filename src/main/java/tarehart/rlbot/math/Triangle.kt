package tarehart.rlbot.math

import kotlin.math.asin
import kotlin.math.sin

/**
 *   A
 *   *
 *   |\
 * b | \ c
 *   |  \
 * C *---* B
 *     a
 *
 * https://www.nayuki.io/page/triangle-solver-javascript
 */
data class Triangle (val sideA: Float, val sideB: Float, val sideC: Float, val angleA: Float, val angleB: Float, val angleC: Float) {


    companion object {

        fun sideSideAngle(sideA: Float, sideB: Float, angleA: Float): Triangle? {
            val ratio = sideA / sin(angleA)
            val temp = sideB / ratio
            if (temp > 1 || angleA >= Math.PI.toFloat() / 2 && sideA <= sideB)
                return null
            else if (temp == 1F || sideA >= sideB) {
                val angleB = asin(temp)
                val angleC = Math.PI.toFloat() - angleA - angleB
                val sideC = ratio * sin(angleC)  // Law of sines

                return Triangle(sideA, sideB, sideC, angleA, angleB, angleC)

            }
            return null
        }
    }
}
