package tarehart.rlbot.routing

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BoostData
import tarehart.rlbot.input.BoostPad
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import java.util.*

object BoostAdvisor {

    private const val MIDFIELD_BOOST_WIDTH = 71.5f
    private const val CORNER_BOOST_WIDTH = 61.5f
    private const val CORNER_BOOST_DEPTH = 82f

    private const val TURN_LIMIT = Math.PI / 4

    private val boostLocations = Arrays.asList(
            Vector3(MIDFIELD_BOOST_WIDTH.toDouble(), 0.0, 0.0),
            Vector3((-MIDFIELD_BOOST_WIDTH).toDouble(), 0.0, 0.0),
            Vector3((-CORNER_BOOST_WIDTH).toDouble(), (-CORNER_BOOST_DEPTH).toDouble(), 0.0),
            Vector3((-CORNER_BOOST_WIDTH).toDouble(), CORNER_BOOST_DEPTH.toDouble(), 0.0),
            Vector3(CORNER_BOOST_WIDTH.toDouble(), (-CORNER_BOOST_DEPTH).toDouble(), 0.0),
            Vector3(CORNER_BOOST_WIDTH.toDouble(), CORNER_BOOST_DEPTH.toDouble(), 0.0)
    )

    fun getBoostWaypoint(car: CarData, boostData: BoostData, waypoint: Vector2) : Vector2? {

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost, 0.0)
        val closestBoost = getClosestBoost(car, distancePlot, boostData, waypoint)
        return closestBoost?.location?.flatten()
    }

    private fun getClosestBoost(car: CarData, distancePlot: DistancePlot, boostData: BoostData, waypoint: Vector2) : BoostPad? {
        val closeFullBoosts = boostData.fullBoosts.sortedBy { it.location.flatten().distance(car.position.flatten()) }.subList(0, 2)
        val closeSmallBoosts = boostData.smallBoosts.sortedBy { it.location.flatten().distance(car.position.flatten()) }.subList(0, 5)


        return (closeFullBoosts + closeSmallBoosts)
                .filter { it.isActive && isEnRoute(car, it.location.flatten(), waypoint) }
                .sortedBy { getGreedyTime(car, distancePlot, it) }
                .firstOrNull()
    }

    private fun isEnRoute(car: CarData, boostLocation: Vector2, waypoint: Vector2) : Boolean {
        val carPosition = car.position.flatten()
        val carToBoost = boostLocation - carPosition
        val boostToWaypoint = waypoint - boostLocation
        val detourAngle = Vector2.angle(carToBoost, boostToWaypoint)
        val initialSteerCorrection = Vector2.angle(car.orientation.noseVector.flatten(), carToBoost)
        return detourAngle + initialSteerCorrection < TURN_LIMIT
    }

    /**
     * Pretends that full boosts are a little closer than they really are, because of greed
     */
    private fun getGreedyTime(car: CarData, distancePlot: DistancePlot, boostPad: BoostPad) : Duration {
        val orientSeconds = AccelerationModel.getSteerPenaltySeconds(car, boostPad.location)
        val distance = car.position.flatten().distance(boostPad.location.flatten())

        val realDuration = distancePlot.getMotionAfterDistance(distance)
                .map { it.time.plusSeconds(orientSeconds) }.orElse(Duration.ofSeconds(100.0))

        val greedBonus = if (boostPad.boostValue > 50) Duration.ofSeconds(1.0) else Duration.ofMillis(0)

        return realDuration - greedBonus
    }

    fun getFullBoostLocation(location: Vector3): Vector3? {
        for (boostLoc in boostLocations) {
            if (boostLoc.distance(location) < 2) {
                return boostLoc
            }
        }
        return null
    }

}