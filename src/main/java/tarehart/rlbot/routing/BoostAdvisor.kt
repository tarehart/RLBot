package tarehart.rlbot.routing

import rlbot.cppinterop.RLBotDll
import rlbot.flat.FieldInfo
import rlbot.flat.GameTickPacket
import tarehart.rlbot.AgentInput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BoostData
import tarehart.rlbot.input.BoostPad
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.util.*

object BoostAdvisor {

    private const val TURN_LIMIT = Math.PI / 4

    private val orderedBoosts = ArrayList<BoostPad>()


    private val fullBoosts = ArrayList<BoostPad>()
    private val smallBoosts = ArrayList<BoostPad>()

    val boostData = BoostData(fullBoosts, smallBoosts)

    private fun loadFieldInfo(fieldInfo: FieldInfo) {

        orderedBoosts.clear()
        fullBoosts.clear()
        smallBoosts.clear()

        for (i in 0 until fieldInfo.boostPadsLength()) {
            val pad = fieldInfo.boostPads(i)
            val spawn = BoostPad(AgentInput.convertVector(pad.location()), pad.isFullBoost)
            orderedBoosts.add(spawn)
            //println("orderBoosts[" + i + "] null?: " + (spawn == null))
            if (spawn.isFullBoost) {
                fullBoosts.add(spawn)
            } else {
                smallBoosts.add(spawn)
            }
        }
    }

    fun loadGameTickPacket(packet: GameTickPacket, currentTime: GameTime) {

        if (packet.boostPadStatesLength() > orderedBoosts.size) {
            loadFieldInfo(RLBotDll.getFieldInfo())
        }

        for (i in 0 until packet.boostPadStatesLength()) {
            val boost = packet.boostPadStates(i)
            val existingPad = orderedBoosts[i] // existingPad is also referenced from the fullBoosts and smallBoosts lists
            if(existingPad == null) {
                println("existingPad unexpectedly null. index: " + i + ", boostPadStatesLength(): " + packet.boostPadStatesLength())
            }
            else {
                existingPad.isActive = boost.isActive

                // TODO: is the boost timer really expressed in milliseconds?
                existingPad.activeTime = if (boost.isActive) currentTime else currentTime + Duration.ofMillis(boost.timer().toLong())
            }
        }
    }

    fun getBoostWaypoint(car: CarData, waypoint: Vector2) : Vector2? {

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost, 0.0)
        val closestBoost = getClosestBoost(car, distancePlot, waypoint)
        return closestBoost?.location?.flatten()
    }

    private fun getClosestBoost(car: CarData, distancePlot: DistancePlot, waypoint: Vector2) : BoostPad? {
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
                ?.time?.plusSeconds(orientSeconds) ?: Duration.ofSeconds(100.0)

        val greedBonus = if (boostPad.isFullBoost) Duration.ofSeconds(1.0) else Duration.ofMillis(0)

        return realDuration - greedBonus
    }
}
