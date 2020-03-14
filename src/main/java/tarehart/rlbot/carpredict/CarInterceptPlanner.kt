package tarehart.rlbot.carpredict

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.ChipStrike
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.time.Duration

object CarInterceptPlanner {

    private val CAR_CONTACT_DISTANCE = 2.5

    fun getCarIntercept(
            carData: CarData,
            enemyPath: CarPath,
            acceleration: DistancePlot): SpaceTime? {

        val myPosition = carData.position

        for (i in 0 until enemyPath.path.size) {
            val slice = enemyPath.path[i]
            val spaceTime = SpaceTime(slice.space, slice.time)
            val strikeProfile = ChipStrike()
            val orientSeconds = SteerUtil.getSteerPenaltySeconds(carData, spaceTime.space)
            val dts = acceleration.getMotionAfterDuration(Duration.between(carData.time, spaceTime.time) - Duration.ofSeconds(orientSeconds), strikeProfile) ?: return null

            val interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space)
            if (dts.distance + CAR_CONTACT_DISTANCE > interceptDistance) {
                if (i > 0) {
                    // Take the average of the current slice and the previous slice to avoid systematic pessimism.
                    val previousSlice = enemyPath.path[i - 1]
                    val timeDiff = Duration.between(previousSlice.time, slice.time).seconds
                    val toCurrentSlice = slice.space.minus(previousSlice.space)
                    return SpaceTime(
                            previousSlice.space + toCurrentSlice.scaled(.5F),
                            previousSlice.time.plusSeconds(timeDiff * .5))

                }
                return null
            }
        }
        return null
    }


}
