package tarehart.rlbot.physics

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.time.Duration

import java.util.ArrayList
import java.util.Optional

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

    fun getEndPoint(): DistanceTimeSpeed {
        return plot[plot.size - 1]
    }

    fun getMotionAfterDuration(time: Duration): Optional<DistanceTimeSpeed> {
        if (time < plot[0].time || time > plot[plot.size - 1].time) {
            return Optional.empty()
        }

        for (i in 0 until plot.size - 1) {
            val current = plot[i]
            val next = plot[i + 1]
            if (next.time > time) {

                val simulationStepSeconds = next.time.seconds - current.time.seconds
                val tweenPoint = (time.seconds - current.time.seconds) / simulationStepSeconds
                val distance = (1 - tweenPoint) * current.distance + tweenPoint * next.distance
                val speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed
                return Optional.of(DistanceTimeSpeed(distance, time, speed))
            }
        }

        return Optional.of(plot[plot.size - 1])
    }

    fun getMotionAfterDistance(distance: Double): Optional<DistanceTimeSpeed> {

        for (i in 0 until plot.size - 1) {
            val current = plot[i]
            val next = plot[i + 1]
            if (next.distance > distance) {
                val simulationStepSeconds = next.time.seconds - current.time.seconds
                val tweenPoint = (distance - current.distance) / (next.distance - current.distance)
                val moment = current.time.plusSeconds(simulationStepSeconds * tweenPoint)
                val speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed
                return Optional.of(DistanceTimeSpeed(distance, moment, speed))
            }
        }
        return Optional.empty()
    }

    fun getTravelTime(distance: Double): Optional<Duration> {
        val motionAt = getMotionAfterDistance(distance)
        return motionAt.map { it.time }
    }

    fun getMotionUponArrival(carData: CarData, destination: Vector3, strikeProfile: StrikeProfile): Optional<DistanceTimeSpeed> {

        val orientSeconds = AccelerationModel.getSteerPenaltySeconds(carData, destination) + strikeProfile.maneuverSeconds
        val distance = carData.position.flatten().distance(destination.flatten())

        return getMotionAfterDistance(distance).map { DistanceTimeSpeed(it.distance, it.time.plusSeconds(orientSeconds), it.speed) }

        // TODO: incorporate the speedup from the strike profile.
    }

    fun getMotionAfterDuration(carData: CarData, target: Vector3, time: Duration, strikeProfile: StrikeProfile): Optional<DistanceTimeSpeed> {

        val orientSeconds = AccelerationModel.getSteerPenaltySeconds(carData, target) + strikeProfile.maneuverSeconds

        val totalSeconds = time.seconds
        val secondsSpentAccelerating = Math.max(0.0, totalSeconds - orientSeconds)

        if (strikeProfile.dodgeSeconds == 0.0 || strikeProfile.speedBoost == 0.0) {
            val motion = getMotionAfterDuration(Duration.ofSeconds(secondsSpentAccelerating))
            return motion.map { DistanceTimeSpeed(it.distance, time, it.speed) }
        }

        val speedupSeconds = strikeProfile.dodgeSeconds
        val speedBoost = strikeProfile.speedBoost
        if (secondsSpentAccelerating < speedupSeconds) {
            // Not enough time for a full strike.
            val beginningSpeed = plot[0].speed
            val increasedSpeed = Math.min(beginningSpeed + speedBoost, AccelerationModel.SUPERSONIC_SPEED)
            return Optional.of(DistanceTimeSpeed(increasedSpeed * secondsSpentAccelerating, time, increasedSpeed))
        }

        val accelSecondsBeforeStrike = secondsSpentAccelerating - speedupSeconds
        val dtsOption = getMotionAfterDuration(Duration.ofSeconds(accelSecondsBeforeStrike))

        return dtsOption.map {
            val beginningSpeed = it.speed
            val increasedSpeed = Math.min(beginningSpeed + speedBoost, AccelerationModel.SUPERSONIC_SPEED)
            DistanceTimeSpeed(it.distance + increasedSpeed * speedupSeconds, time, increasedSpeed)
        }
    }
}
