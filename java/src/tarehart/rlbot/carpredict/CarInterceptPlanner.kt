package tarehart.rlbot.carpredict

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.time.Duration

import java.util.Optional

object CarInterceptPlanner {

    private val CAR_CONTACT_DISTANCE = 5

    fun getCarIntercept(
            carData: CarData,
            enemyPath: CarPath,
            acceleration: DistancePlot): SpaceTime? {

        val myPosition = carData.position

        for (i in 0 until enemyPath.slices.size) {
            val slice = enemyPath.slices[i]
            val spaceTime = SpaceTime(slice.space, slice.time)
            val strikeProfile = StrikeProfile()
            val motionAt = acceleration.getMotionAfterDuration(carData, spaceTime.space, Duration.between(carData.time, spaceTime.time), strikeProfile)
            if (motionAt.isPresent) {
                val dts = motionAt.get()
                val interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space)
                if (dts.distance + CAR_CONTACT_DISTANCE > interceptDistance) {
                    if (i > 0) {
                        // Take the average of the current slice and the previous slice to avoid systematic pessimism.
                        val previousSlice = enemyPath.slices[i - 1]
                        val timeDiff = Duration.between(previousSlice.time, slice.time).seconds
                        val toCurrentSlice = slice.space.minus(previousSlice.space)
                        return SpaceTime(
                                previousSlice.space + toCurrentSlice.scaled(.5),
                                previousSlice.time.plusSeconds(timeDiff * .5))

                    }
                    return null
                }
            } else {
                return null
            }
        }
        return null
    }


}
