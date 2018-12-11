package tarehart.rlbot.routing

import rlbot.cppinterop.RLBotDll
import rlbot.flat.FieldInfo
import rlbot.flat.GameTickPacket
import tarehart.rlbot.AgentInput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BoostData
import tarehart.rlbot.input.BoostPad
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.awt.Color
import java.util.*

object BoostAdvisor {

    private const val TURN_LIMIT = Math.PI * .3
    private const val FULL_BOOST_TURN_LIMIT = Math.PI * .6

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
            val spawn = BoostPad(Vector3.fromRlbot(pad.location()), pad.isFullBoost)
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
            synchronized (boostData) {
                loadFieldInfo(RLBotDll.getFieldInfo())
            }
        }

        for (i in 0 until packet.boostPadStatesLength()) {
            val boost = packet.boostPadStates(i)
            try {
                val existingPad = orderedBoosts[i] // existingPad is also referenced from the fullBoosts and smallBoosts lists
                existingPad.isActive = boost.isActive

                // TODO: is the boost timer really expressed in milliseconds?
                existingPad.activeTime = if (boost.isActive) currentTime else getBoostEta(boost.timer(), existingPad.isFullBoost, currentTime)
            } catch (e: IndexOutOfBoundsException) {
                println("existingPad unexpectedly null. index: " + i + ", boostPadStatesLength(): " + packet.boostPadStatesLength())
            }
        }
    }

    fun getBoostEta(boostTimer:Float, fullBoost:Boolean, currentTime: GameTime): GameTime {
        val respawnTime = if (fullBoost) 10.0 else 5.0
        return currentTime + Duration.ofSeconds(respawnTime - boostTimer)
    }

    fun getBoostWaypoint(car: CarData, waypoint: Vector2) : Vector2? {

        if (BoostAdvisor.orderedBoosts.size < 5) {
            return waypoint
        }

        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost, 0.0)
        val closestBoost = getClosestBoost(car, distancePlot, waypoint)
        closestBoost?.location?.flatten()?.let {
            RenderUtil.drawCircle(car.renderer, Circle(it, 2.0), 0.0, Color.GREEN)
            return it
        }
        return null
    }

    private fun getClosestBoost(car: CarData, distancePlot: DistancePlot, waypoint: Vector2) : BoostPad? {
        val closeFullBoosts = boostData.fullBoosts.sortedBy { it.location.flatten().distance(car.position.flatten()) }.subList(0, 2)
        val closeSmallBoosts = boostData.smallBoosts.sortedBy { it.location.flatten().distance(car.position.flatten()) }.subList(0, 5)


        return (closeFullBoosts + closeSmallBoosts)
                .filter { it.isActive && isEnRoute(car, it, waypoint) }
                .sortedBy { getGreedyTime(car, distancePlot, it) }
                .firstOrNull()
    }

    private fun isEnRoute(car: CarData, boostPad: BoostPad, waypoint: Vector2) : Boolean {
        val carPosition = car.position.flatten()
        val boostPosition = boostPad.location.flatten()
        val carToBoost = boostPosition - carPosition
        val boostToWaypoint = waypoint - boostPosition
        val detourDistance = carToBoost.magnitude() + boostToWaypoint.magnitude() - (waypoint - carPosition).magnitude()
        if (detourDistance > 20) {
            return false
        }

        val detourAngle = Vector2.angle(carToBoost, boostToWaypoint)
        val initialSteerCorrection = Vector2.angle(car.orientation.noseVector.flatten(), carToBoost)
        val turnLimit = if (boostPad.isFullBoost) FULL_BOOST_TURN_LIMIT else TURN_LIMIT
        return detourAngle + initialSteerCorrection < turnLimit
    }

    /**
     * Pretends that full boosts are a little closer than they really are, because of greed
     */
    private fun getGreedyTime(car: CarData, distancePlot: DistancePlot, boostPad: BoostPad) : Duration {
        val orientSeconds = AccelerationModel.getSteerPenaltySeconds(car, boostPad.location)
        val distance = car.position.flatten().distance(boostPad.location.flatten())

        val realDuration = distancePlot.getMotionAfterDistance(distance)
                ?.time?.plusSeconds(orientSeconds) ?: Duration.ofSeconds(100.0)

        val greedBonus = if (boostPad.isFullBoost) Duration.ofSeconds(0.5) else Duration.ofMillis(0)

        return realDuration - greedBonus
    }
}
