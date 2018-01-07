package tarehart.rlbot.intercept

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.*
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.util.Optional
import java.util.function.BiPredicate
import java.util.function.Function

object InterceptCalculator {

    fun getInterceptOpportunityAssumingMaxAccel(carData: CarData, ballPath: BallPath, boostBudget: Double): Optional<Intercept> {
        val plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4.0), boostBudget)

        return getInterceptOpportunity(carData, ballPath, plot)
    }

    fun getInterceptOpportunity(carData: CarData, ballPath: BallPath, acceleration: DistancePlot): Optional<Intercept> {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, Vector3(), { _, _ ->  true })
    }

    @JvmOverloads
    fun getFilteredInterceptOpportunity(
            carData: CarData,
            ballPath: BallPath,
            acceleration: DistancePlot,
            interceptModifier: Vector3,
            predicate: (CarData, SpaceTime) -> Boolean,
            strikeProfileFn: (Vector3) -> StrikeProfile = { StrikeProfile() }): Optional<Intercept> {

        val groundNormal = Vector3(0.0, 0.0, 1.0)
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, interceptModifier, predicate, strikeProfileFn, groundNormal)
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param acceleration
     * @param interceptModifier an offset from the ball position that the car is trying to reach
     * @param predicate determines whether a particular ball slice is eligible for intercept
     * @param strikeProfileFn a description of how the car will move during the final moments of the intercept
     * @param planeNormal the normal of the plane that the car is driving on for this intercept.
     * @return
     */
    fun getFilteredInterceptOpportunity(
            carData: CarData,
            ballPath: BallPath,
            acceleration: DistancePlot,
            interceptModifier: Vector3,
            predicate: (CarData, SpaceTime) -> Boolean,
            strikeProfileFn: (Vector3) -> StrikeProfile,
            planeNormal: Vector3): Optional<Intercept> {

        val myPosition = carData.position
        var firstMomentInRange: GameTime? = null
        var previousRangeDeficiency = 0.0

        for (i in 0 until ballPath.slices.size) {
            val slice = ballPath.slices[i]
            val spaceTime = SpaceTime(slice.space.plus(interceptModifier), slice.time)
            val strikeProfile = strikeProfileFn.invoke(spaceTime.space)
            val motionAt = acceleration.getMotionAfterDuration(carData, spaceTime.space, Duration.between(carData.time, spaceTime.time), strikeProfile)
            if (!motionAt.isPresent) return Optional.empty()

            val dts = motionAt.get()
            val interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space, planeNormal)
            val rangeDeficiency = interceptDistance - dts.distance
            if (rangeDeficiency <= 0) {
                if (firstMomentInRange == null) {
                    firstMomentInRange = spaceTime.time
                }
                if (predicate.invoke(carData, spaceTime)) {

                    val tweenedSlice = getTweenedSlice(ballPath, slice, i, rangeDeficiency, previousRangeDeficiency)
                    val boostNeeded = boostNeededForAerial(spaceTime.space.z)
                    val spareTime = if (tweenedSlice.time > firstMomentInRange) tweenedSlice.time - firstMomentInRange else Duration.ofMillis(0)

                    return Optional.of(Intercept(tweenedSlice.space + interceptModifier, tweenedSlice.time, boostNeeded, strikeProfile, acceleration, spareTime))
                }
            }
            previousRangeDeficiency = rangeDeficiency
        }

        // No slices in the ball slices were in range and satisfied the predicate
        return Optional.empty()
    }

    private fun boostNeededForAerial(height: Double) : Double {
        return if (height > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL else 0.0
    }

    private fun getTweenedSlice(ballPath: BallPath, currentSlice: BallSlice, currentSliceIndex: Int, currentShortfall: Double, previousShortfall: Double): BallSlice {
        var tweenPoint = 1.0
        if (previousShortfall > 0) {
            tweenPoint = previousShortfall / (previousShortfall - currentShortfall)
        }
        val previousSliceTime = ballPath.slices[currentSliceIndex - 1].time
        val sliceSeconds = Duration.between(previousSliceTime, currentSlice.time).seconds
        val moment = previousSliceTime.plusSeconds(sliceSeconds * tweenPoint)
        return ballPath.getMotionAt(moment).orElse(currentSlice)
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
            launchMoment: GameTime): Optional<SpaceTime> {

        val myPosition = carData.position

        for ((space, time) in ballPath.slices) {
            val intercept = SpaceTime(space.plus(interceptModifier), time)

            val timeSinceLaunch = Duration.between(launchMoment, carData.time)
            val duration = Duration.between(carData.time, time)
            val zComponent = AerialMath.getDesiredZComponentBasedOnAccel(intercept.space.z, duration, timeSinceLaunch, carData)
            val desiredNoseAngle = Math.asin(zComponent)
            val currentNoseAngle = Math.asin(carData.orientation.noseVector.z)
            val currentAngleFactor = Math.min(1.0, 1 / duration.seconds)
            val averageNoseAngle = currentNoseAngle * currentAngleFactor + desiredNoseAngle * (1 - currentAngleFactor)

            val acceleration = AccelerationModel.simulateAirAcceleration(carData, duration, Math.cos(averageNoseAngle))
            val strikeProfile = if (duration.compareTo(MidairStrikeStep.MAX_TIME_FOR_AIR_DODGE) < 0 && averageNoseAngle < .5)
                StrikeProfile(0.0, 0.0, 10.0, .15, StrikeProfile.Style.AERIAL)
            else
                InterceptStep.AERIAL_STRIKE_PROFILE

            val motionAt = acceleration.getMotionAfterDuration(
                    carData, intercept.space, Duration.between(carData.time, intercept.time), strikeProfile)

            if (motionAt.isPresent) {
                val dts = motionAt.get()
                val interceptDistance = VectorUtil.flatDistance(myPosition, intercept.space)
                if (dts.distance > interceptDistance) {
                    return Optional.of(intercept)
                }
            } else {
                return Optional.empty()
            }
        }
        return Optional.empty()
    }
}
