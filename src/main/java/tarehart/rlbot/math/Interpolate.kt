package tarehart.rlbot.math

object Interpolate {

    fun getValue(curve: List<Pair<Float, Float>>, independentVariable: Float): Float? {

        if (curve.size < 2) {
            throw IllegalArgumentException("Curve must have a size of at least 2")
        }

        if (independentVariable < curve[0].first) {
            return null
        }

        if (independentVariable > curve.last().first) {
            return null
        }

        for (i in 1 until curve.size) {
            val lower = curve[i - 1]
            val higher = curve[i]

            if (lower.first <= independentVariable && higher.first >= independentVariable ) {
                val difference = higher.first - lower.first
                val relativeScale = (independentVariable - lower.first) / difference
                return lower.second + relativeScale * (higher.second - lower.second)
            }
        }
        throw IllegalStateException("It should be impossible to get here")
    }

    fun getInverse(curve: List<Pair<Float, Float>>, dependentVariable: Float): Float? {

        if (curve.size < 2) {
            throw IllegalArgumentException("Curve must have a size of at least 2")
        }

        for (i in 1 until curve.size) {
            val lower = curve[i - 1]
            val higher = curve[i]

            if (isBetween(dependentVariable, lower.second, higher.second)) {
                val difference = higher.second - lower.second
                val relativeScale = (dependentVariable - lower.second) / difference
                return lower.first + relativeScale * (higher.first - lower.first)
            }
        }
        return null
    }

    fun isBetween(num: Float, bound1: Float, bound2: Float): Boolean {
        return listOf(num, bound1, bound2).sorted()[1] == num
    }

}
