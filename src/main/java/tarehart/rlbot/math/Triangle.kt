package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector2

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

        fun dodgePosition(carPosition: Vector2, carAtContact: Vector2, dodgeDeflectionAngle: Double, dodgeTravel: Double): Vector2? {
            val toContact = carAtContact - carPosition
            val triangle = sideSideAngle(toContact.magnitude(), dodgeTravel, Math.abs(dodgeDeflectionAngle)) ?: return null

            // We want the location of pt A as the launch position.
            // carPosition is at B
            // carAtContact is at C

            // Let's find the absolute angle of side B, by getting the absolute angle of side A and then adding angleC to it.
            val sideAAngle = Math.atan2(toContact.y, toContact.x)
            val sideBAngle = sideAAngle + triangle.angleB * Math.signum(dodgeDeflectionAngle) // TODO: is this inverted?
            val toDodge = Vector2(Math.cos(sideBAngle), Math.sin(sideBAngle)).scaled(triangle.sideC)
            return carPosition + toDodge

        }

        fun sideSideAngle(knownSide: Double, partialSide: Double, knownAngle: Double): Triangle? {
            val ratio = knownSide / Math.sin(knownAngle)
            val temp = partialSide / ratio
            if (temp > 1 || knownAngle >= Math.PI / 2 && knownSide <= partialSide)
                return null
            else if (temp == 1.0 || knownSide >= partialSide) {
                val partialAngle = Math.asin(temp)
                val unknownAngle = Math.PI - knownAngle - partialAngle
                val unknownSide = ratio * Math.sin(unknownAngle)  // Law of sines

                return Triangle(knownSide, partialSide, unknownSide, knownAngle, partialAngle, unknownAngle)

            }
            return null
        }
    }
}