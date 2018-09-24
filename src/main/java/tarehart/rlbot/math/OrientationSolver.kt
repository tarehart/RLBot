package tarehart.rlbot.math

import org.ejml.simple.SimpleMatrix

class OrentationSolver {
    val ALPHA_MAX = 9.0

    var found = false
    var target = 0

    /**
     * I don't understand the point of this yet:
     * http://www.wolframalpha.com/input/?i=y+%3D+((x+-+pi)+mod+(2+*+pi))+%2B+pi
     *
     * It clamps to a domain of PI to 3PI, which does not seem very useful
     */
    fun periodic(x: Double): Double {
        return ((x - Math.PI) % (2 * Math.PI)) + Math.PI
    }

    fun q(x: Double): Double {
        return 1.0 - (1.0 / (1.0 + 500.0 * x * x))
    }

    fun r(delta: Double, v: Double): Double {
        return delta - 0.5 * Math.signum(v) * v * v / ALPHA_MAX
    }

    fun controller(delta: Double, v: Double, dt: Double): Double {
        val ri = r(delta, v)
        val alpha = Math.signum(ri) * ALPHA_MAX
        val rf = r(delta - v * dt, v + alpha * dt)

        // use a single step of secant method to improve
        // the acceleration when residual changes sign
        if (ri * rf < 0.0) {
            return alpha * (2.0 * (ri / (ri - rf)) - 1)
        }

        return alpha
    }

    fun find_landing_orientation(numPoints: Int): Mat3 {

    }

}
