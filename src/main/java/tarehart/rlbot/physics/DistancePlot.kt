package tarehart.rlbot.physics

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.Duration
import java.util.*

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

    fun getMotionAfterDistance(distance: Double): DistanceTimeSpeed? {

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
    fun getMotionUponSpeed(targetSpeed: Double): DistanceTimeSpeed? {

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

    fun getTravelTime(distance: Double): Duration? {
        val motionAt = getMotionAfterDistance(distance)
        return motionAt?.time
    }

    fun getMotionUponArrival(carData: CarData, destination: Vector3, strikeProfile: StrikeProfile): DistanceTimeSpeed? {

        val orientSeconds = AccelerationModel.getSteerPenaltySeconds(carData, destination)
        val distance = carData.position.flatten().distance(destination.flatten())

        return getMotionAfterDistance(distance)?.let { DistanceTimeSpeed(it.distance, it.time.plusSeconds(orientSeconds), it.speed) }

        // TODO: incorporate the speedup from the strike profile.
    }

    fun getMotionAfterDuration(time: Duration, strikeProfile: StrikeProfile): DistanceTimeSpeed? {

        val totalSeconds = Math.max(time.seconds, 0.0)
        val secondsSpentAccelerating = totalSeconds

        if (strikeProfile.postDodgeTime == Duration.ZERO || strikeProfile.speedBoost == 0.0) {
            val motion = getMotionAfterDuration(Duration.ofSeconds(secondsSpentAccelerating))
            return motion?.let { DistanceTimeSpeed(it.distance, time, it.speed) }
        }

        val speedupSeconds = strikeProfile.postDodgeTime.seconds
        val speedBoost = strikeProfile.speedBoost
        if (secondsSpentAccelerating < speedupSeconds) {
            // Not enough time for a full strike.
            val beginningSpeed = plot[0].speed
            val increasedSpeed = Math.min(beginningSpeed + speedBoost, AccelerationModel.SUPERSONIC_SPEED)
            return DistanceTimeSpeed(increasedSpeed * secondsSpentAccelerating, time, increasedSpeed)
        }

        val accelSecondsBeforeStrike = secondsSpentAccelerating - speedupSeconds
        val dtsOption = getMotionAfterDuration(Duration.ofSeconds(accelSecondsBeforeStrike))

        return dtsOption?.let {
            val beginningSpeed = it.speed
            val increasedSpeed = Math.min(beginningSpeed + speedBoost, AccelerationModel.SUPERSONIC_SPEED)
            DistanceTimeSpeed(it.distance + increasedSpeed * speedupSeconds, time, increasedSpeed)
        }
    }
}
