package tarehart.rlbot.physics

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DistancePlot(start: DistanceTimeSpeed) {

    private var plot = ArrayList<DistanceTimeSpeed>()

    val slices: List<DistanceTimeSpeed>
        get() = plot

    init {
        plot.add(start)
    }

    fun addSlice(dts: DistanceTimeSpeed) {
        plot.add(dts)
    }

    fun getStartPoint(): DistanceTimeSpeed {
        return plot[0]
    }

    fun getEndPoint(): DistanceTimeSpeed {
        return plot[plot.size - 1]
    }

    fun getMotionAfterDuration(time: Duration): DistanceTimeSpeed? {
        if (time < plot[0].time || time > plot[plot.size - 1].time) {
            return null
        }

        for (i in 0 until plot.size - 1) {
            val current = plot[i]
            val next = plot[i + 1]
            if (next.time > time) {

                val simulationStepSeconds = next.time.seconds - current.time.seconds
                val tweenPoint = (time.seconds - current.time.seconds) / simulationStepSeconds
                val distance = (1 - tweenPoint) * current.distance + tweenPoint * next.distance
                val speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed
                return DistanceTimeSpeed(distance, time, speed)
            }
        }

        return plot[plot.size - 1]
    }

    fun getMotionAfterDistance(distance: Float): DistanceTimeSpeed? {

        for (i in 0 until plot.size - 1) {
            val current = plot[i]
            val next = plot[i + 1]
            if (next.distance > distance) {
                val simulationStepSeconds = next.time.seconds - current.time.seconds
                val tweenPoint = (distance - current.distance) / (next.distance - current.distance)
                val moment = current.time.plusSeconds(simulationStepSeconds * tweenPoint)
                val speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed
                return DistanceTimeSpeed(distance, moment, speed)
            }
        }
        return null
    }

    /**
     * Only applies to forward acceleration. Will return null if we're already going faster than
     * the target speed, or if the target speed is impossible to attain.
     */
    fun getMotionUponSpeed(targetSpeed: Float): DistanceTimeSpeed? {

        val currentSpeed = plot[0].speed
        if (currentSpeed > targetSpeed) return null


        for (i in 0 until plot.size - 1) {
            val current = plot[i]
            val next = plot[i + 1]
            if (next.speed > targetSpeed) {

                val simulationStepSeconds = next.time.seconds - current.time.seconds
                val tweenPoint = (targetSpeed - current.speed) / (next.speed - current.speed)
                val distance = (1 - tweenPoint) * current.distance + tweenPoint * next.distance
                val time = current.time.plusSeconds(simulationStepSeconds * tweenPoint)
                return DistanceTimeSpeed(distance, time, targetSpeed)
            }
        }

        return null
    }

    fun getTravelTime(distance: Float): Duration? {
        val motionAt = getMotionAfterDistance(distance)
        return motionAt?.time
    }

    fun getMaximumRange(car: CarData, direction: Vector3, travelTime: Duration): Float? {
        val orientSeconds = SteerUtil.getSteerPenaltySeconds(car, direction)
        return getMotionAfterDuration(Duration.ofSeconds(max(0.0, travelTime.seconds - orientSeconds)))?.distance
    }

    fun getMotionUponArrival(carData: CarData, destination: Vector3): DistanceTimeSpeed? {

        val orientSeconds = SteerUtil.getSteerPenaltySeconds(carData, destination)
        val distance = carData.position.flatten().distance(destination.flatten())

        return getMotionAfterDistance(distance)?.let { DistanceTimeSpeed(it.distance, it.time.plusSeconds(orientSeconds), it.speed) }
    }

    fun getMotionAfterDuration(time: Duration, strikeProfile: StrikeProfile): DistanceTimeSpeed? {

        val secondsSpentAccelerating = max(time.seconds - strikeProfile.strikeDuration.seconds, 0F)
        val secondsForPreDodge = min(strikeProfile.preDodgeTime.seconds, max(0F, time.seconds))
        val secondsForPostDodge = max(0F, min(time.seconds - strikeProfile.preDodgeTime.seconds, strikeProfile.postDodgeTime.seconds))

        val speedBoost = if (time < strikeProfile.strikeDuration) strikeProfile.speedBoost else 0F

        val dts = getMotionAfterDuration(Duration.ofSeconds(secondsSpentAccelerating)) ?: return null
        val increasedSpeed = min(dts.speed + speedBoost, AccelerationModel.SUPERSONIC_SPEED)
        val distance = dts.distance + secondsForPreDodge * dts.speed + secondsForPostDodge * increasedSpeed
        return DistanceTimeSpeed(distance, time, increasedSpeed)
    }
}
