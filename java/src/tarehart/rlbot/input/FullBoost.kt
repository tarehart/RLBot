package tarehart.rlbot.input

import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

import java.util.Arrays
import java.util.Optional

class FullBoost(val location: Vector3, val isActive: Boolean, val activeTime: GameTime) {
    companion object {

        private const val MIDFIELD_BOOST_WIDTH = 71.5f
        private const val CORNER_BOOST_WIDTH = 61.5f
        private const val CORNER_BOOST_DEPTH = 82f

        private val boostLocations = Arrays.asList(
                Vector3(MIDFIELD_BOOST_WIDTH.toDouble(), 0.0, 0.0),
                Vector3((-MIDFIELD_BOOST_WIDTH).toDouble(), 0.0, 0.0),
                Vector3((-CORNER_BOOST_WIDTH).toDouble(), (-CORNER_BOOST_DEPTH).toDouble(), 0.0),
                Vector3((-CORNER_BOOST_WIDTH).toDouble(), CORNER_BOOST_DEPTH.toDouble(), 0.0),
                Vector3(CORNER_BOOST_WIDTH.toDouble(), (-CORNER_BOOST_DEPTH).toDouble(), 0.0),
                Vector3(CORNER_BOOST_WIDTH.toDouble(), CORNER_BOOST_DEPTH.toDouble(), 0.0)
        )

        fun getFullBoostLocation(location: Vector3): Optional<Vector3> {
            for (boostLoc in boostLocations) {
                if (boostLoc.distance(location) < 2) {
                    return Optional.of(boostLoc)
                }
            }
            return Optional.empty()
        }
    }

}
