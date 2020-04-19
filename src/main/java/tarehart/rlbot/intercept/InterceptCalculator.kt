package tarehart.rlbot.intercept

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.ChipStrike
import tarehart.rlbot.intercept.strike.CustomStrike
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.intercept.strike.Style
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

object InterceptCalculator {

    fun getInterceptOpportunityAssumingMaxAccel(carData: CarData, ballPath: BallPath, boostBudget: Float): Intercept? {
        val plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4.0), boostBudget)

        return getInterceptOpportunity(carData, ballPath, plot)
    }

    fun getInterceptOpportunity(carData: CarData, ballPath: BallPath, acceleration: DistancePlot): Intercept? {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, Vector3(), { _, _ ->  true })
    }

    /**
     * The predicate does NOT need to account for vertical accessibility, internally we will already
     * ask the chosen strike profile about that.
     */
    @JvmOverloads
    fun getFilteredInterceptOpportunity(
            carData: CarData,
            ballPath: BallPath,
            acceleration: DistancePlot,
            sliceToCar: Vector3,
            predicate: (CarData, SpaceTime) -> Boolean,
            strikeProfileFn: (BallSlice) -> StrikeProfile = { ChipStrike() }): Intercept? {

        val groundNormal = Vector3(0.0, 0.0, 1.0)
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, sliceToCar, predicate, strikeProfileFn, groundNormal)
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param acceleration
     * @param sliceToCar an offset from the ball position that the car is trying to reach
     * @param spatialPredicate determines whether a particular ball slice is eligible for intercept
     * @param strikeProfileFn a description of how the car will move during the final moments of the intercept
     * @param planeNormal the normal of the plane that the car is driving on for this intercept.
     * @return
     */
    fun getFilteredInterceptOpportunity(
            carData: CarData,
            ballPath: BallPath,
            acceleration: DistancePlot,
            sliceToCar: Vector3,
            spatialPredicate: (CarData, SpaceTime) -> Boolean,
            strikeProfileFn: (BallSlice) -> StrikeProfile,
            planeNormal: Vector3): Intercept? {

        val myPosition = carData.position
        var firstMomentInRange: GameTime? = null
        var previousRangeDeficiency = 0F

        for (i in 0 until ballPath.slices.size) {
            val slice = ballPath.slices[i]
            val spaceTime = SpaceTime(slice.space.plus(sliceToCar), slice.time)
            val strikeProfile = strikeProfileFn.invoke(slice)
            val orientDuration = if (strikeProfile.isForward) AccelerationModel.getOrientDuration(carData, spaceTime.space) else Duration.ofMillis(0)
            val dts = acceleration.getMotionAfterDuration(Duration.between(carData.time, spaceTime.time) - orientDuration, strikeProfile) ?: return null

            val interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space, planeNormal)
            val rangeDeficiency = interceptDistance - dts.distance
            if (rangeDeficiency <= 0) {
                if (firstMomentInRange == null) {
                    firstMomentInRange = spaceTime.time
                }
                if (spatialPredicate.invoke(carData, spaceTime) && strikeProfile.isVerticallyAccessible(carData, spaceTime)) {

                    val tweenedSlice = getTweenedSlice(ballPath, slice, i, rangeDeficiency, previousRangeDeficiency)
                    val boostNeeded = StrikePlanner.boostNeededForAerial(spaceTime.space.z)
                    val spatialPredicateFailurePeriod =
                            if (tweenedSlice.time > firstMomentInRange) tweenedSlice.time - firstMomentInRange
                            else Duration.ofMillis(0)

                    return Intercept(
                            tweenedSlice.space + sliceToCar,
                            tweenedSlice.time,
                            boostNeeded,
                            strikeProfile,
                            acceleration,
                            spatialPredicateFailurePeriod,
                            tweenedSlice,
                            dts)
                }
            }
            previousRangeDeficiency = rangeDeficiency
        }

        // No slices in the ball slices were in range and satisfied the predicate
        return null
    }

    private fun getTweenedSlice(ballPath: BallPath, currentSlice: BallSlice, currentSliceIndex: Int, currentShortfall: Float, previousShortfall: Float): BallSlice {

        if (currentSliceIndex == 0) {
            return currentSlice
        }

        var tweenPoint = 1F
        if (previousShortfall > 0) {
            tweenPoint = previousShortfall / (previousShortfall - currentShortfall)
        }
        val previousSliceTime = ballPath.slices[currentSliceIndex - 1].time
        val sliceSeconds = Duration.between(previousSliceTime, currentSlice.time).seconds
        val moment = previousSliceTime.plusSeconds(sliceSeconds * tweenPoint)
        return ballPath.getMotionAt(moment) ?: currentSlice
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param interceptModifier an offset from the ball position that the car is trying to reach
     * @return
     */
    fun getAerialIntercept(
            carData: CarData,
            ballPath: BallPath,
            interceptModifier: Vector3,
            launchMoment: GameTime,
            spatialPredicate: (CarData, SpaceTime) -> Boolean): Intercept? {

        val myPosition = carData.position

        var previousFinesseTime: Duration = Duration.ofSeconds(-100.0)

        for (slice in ballPath.slices) {
            val intercept = SpaceTime(slice.space.plus(interceptModifier), slice.time)

            val timeSinceLaunch = Duration.between(launchMoment, carData.time)
            val duration = Duration.between(carData.time, slice.time)
            val acceleration = AccelerationModel.simulateAirAcceleration(carData, duration, 1F)

            // We're already in the air, so don't model any hang time.
            val strikeProfile = CustomStrike(Duration.ZERO, Duration.ZERO, 0F, Style.AERIAL)

            val dts = acceleration.getMotionAfterDuration(Duration.between(carData.time, intercept.time), strikeProfile) ?: return null

            val interceptDistance = VectorUtil.flatDistance(myPosition, intercept.space)

            if (dts.distance > interceptDistance && spatialPredicate.invoke(carData, intercept)) {

                val courseCorrection = AerialMath.calculateAerialCourseCorrection(
                        CarSlice(carData), intercept, modelJump = false, secondsSinceJump = timeSinceLaunch.seconds)

                val aerialTime = AerialMath.calculateAerialTimeNeeded(courseCorrection)
                val aerialFinesseTime = intercept.time - carData.time - aerialTime
                if (aerialFinesseTime.seconds > 0.2 || aerialFinesseTime < previousFinesseTime) {

                    return Intercept(
                            intercept.space,
                            intercept.time,
                            airBoost = 0F,
                            strikeProfile = ChipStrike(),
                            distancePlot = acceleration,
                            spatialPredicateFailurePeriod = Duration.ofMillis(0),
                            ballSlice = slice,
                            accelSlice = dts)
                }

                previousFinesseTime = aerialFinesseTime
            }

        }
        return null
    }

    fun getSoonestInterceptCheaply(carData: CarData, ballPath: BallPath, acceleration: DistancePlot): Intercept? {
        val myPosition = carData.position

        for (i in 0 until ballPath.slices.size step 4) {
            val slice = ballPath.slices[i]
            val spaceTime = SpaceTime(slice.space, slice.time)
            val strikeProfile = ChipStrike()
            val orientDuration = AccelerationModel.getOrientDuration(carData, spaceTime.space)
            val dts = acceleration.getMotionAfterDuration(Duration.between(carData.time, spaceTime.time) - orientDuration, strikeProfile) ?: return null

            val interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space)
            val rangeDeficiency = interceptDistance - dts.distance
            if (rangeDeficiency <= 0) {
                    return Intercept(
                            spaceTime.space,
                            spaceTime.time,
                            0F,
                            strikeProfile,
                            acceleration,
                            Duration.ofMillis(0),
                            slice,
                            dts)

            }
        }

        // No slices in the ball slices were in range and satisfied the predicate
        return null
    }
}
