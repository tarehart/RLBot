package tarehart.rlbot.math

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
data class Triangle (val sideA: Double, val sideB: Double, val sideC: Double, val angleA: Double, val angleB: Double, val angleC: Double) {


    companion object {



        fun sideSideAngle(sideA: Double, sideB: Double, angleA: Double): Triangle? {
            val ratio = sideA / Math.sin(angleA)
            val temp = sideB / ratio
            if (temp > 1 || angleA >= Math.PI / 2 && sideA <= sideB)
                return null
            else if (temp == 1.0 || sideA >= sideB) {
                val angleB = Math.asin(temp)
                val angleC = Math.PI - angleA - angleB
                val sideC = ratio * Math.sin(angleC)  // Law of sines

                return Triangle(sideA, sideB, sideC, angleA, angleB, angleC)

            }
            return null
        }
    }
}