package tarehart.rlbot.intercept

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.strike.ChipStrike
import tarehart.rlbot.intercept.strike.CustomStrike
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.routing.PrecisionPlan
import tarehart.rlbot.routing.StrikeRoutePart
import tarehart.rlbot.steps.strikes.DirectedKickUtil
import tarehart.rlbot.steps.strikes.KickStrategy
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

object InterceptCalculator {

    fun getInterceptOpportunityAssumingMaxAccel(carData: CarData, ballPath: BallPath, boostBudget: Double): Intercept? {
        val plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4.0), boostBudget)

        return getInterceptOpportunity(carData, ballPath, plot)
    }

    fun getInterceptOpportunity(carData: CarData, ballPath: BallPath, acceleration: DistancePlot): Intercept? {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, Vector3(), { _, _ ->  true })
    }

    @JvmOverloads
    fun getFilteredInterceptOpportunity(
            carData: CarData,
            ballPath: BallPath,
            acceleration: DistancePlot,
            interceptModifier: Vector3,
            predicate: (CarData, SpaceTime) -> Boolean,
            strikeProfileFn: (Double) -> StrikeProfile = { ChipStrike() }): Intercept? {

        val groundNormal = Vector3(0.0, 0.0, 1.0)
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, interceptModifier, predicate, strikeProfileFn, groundNormal)
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param acceleration
     * @param interceptModifier an offset from the ball position that the car is trying to reach
     * @param spatialPredicate determines whether a particular ball slice is eligible for intercept
     * @param strikeProfileFn a description of how the car will move during the final moments of the intercept
     * @param planeNormal the normal of the plane that the car is driving on for this intercept.
     * @return
     */
    fun getFilteredInterceptOpportunity(
            carData: CarData,
            ballPath: BallPath,
            acceleration: DistancePlot,
            interceptModifier: Vector3,
            spatialPredicate: (CarData, SpaceTime) -> Boolean,
            strikeProfileFn: (Double) -> StrikeProfile,
            planeNormal: Vector3): Intercept? {

        val myPosition = carData.position
        var firstMomentInRange: GameTime? = null
        var previousRangeDeficiency = 0.0

        for (i in 0 until ballPath.slices.size) {
            val slice = ballPath.slices[i]
            val spaceTime = SpaceTime(slice.space.plus(interceptModifier), slice.time)
            val strikeProfile = strikeProfileFn.invoke(spaceTime.space.z)
            val orientDuration = if (strikeProfile.isForward) AccelerationModel.getOrientDuration(carData, spaceTime.space) else Duration.ofMillis(0)
            val dts = acceleration.getMotionAfterDuration(Duration.between(carData.time, spaceTime.time) - orientDuration, strikeProfile) ?: return null

            val interceptDistance = VectorUtil.flatDistance(myPosition, spaceTime.space, planeNormal)
            val rangeDeficiency = interceptDistance - dts.distance
            if (rangeDeficiency <= 0) {
                if (firstMomentInRange == null) {
                    firstMomentInRange = spaceTime.time
                }
                if (spatialPredicate.invoke(carData, spaceTime)) {

                    val tweenedSlice = getTweenedSlice(ballPath, slice, i, rangeDeficiency, previousRangeDeficiency)
                    val boostNeeded = boostNeededForAerial(spaceTime.space.z)
                    val spareTime = if (tweenedSlice.time > firstMomentInRange) tweenedSlice.time - firstMomentInRange else Duration.ofMillis(0)

                    return Intercept(
                            tweenedSlice.space + interceptModifier,
                            tweenedSlice.time,
                            boostNeeded,
                            strikeProfile,
                            acceleration,
                            spareTime,
                            tweenedSlice,
                            dts)
                }
            }
            previousRangeDeficiency = rangeDeficiency
        }

        // No slices in the ball slices were in range and satisfied the predicate
        return null
    }

    /**
     *
     * @param carData
     * @param ballPath
     * @param acceleration
     * @param interceptModifier an offset from the ball position that the car is trying to reach
     * @param spatialPredicate determines whether a particular ball slice is eligible for intercept
     * @param strikeProfileFn a description of how the car will move during the final moments of the intercept
     * @param planeNormal the normal of the plane that the car is driving on for this intercept.
     * @return
     */
    fun getRouteAwareIntercept(
            carData: CarData,
            ballPath: BallPath,
            acceleration: DistancePlot,
            spatialPredicate: (CarData, SpaceTime, StrikeProfile) -> Boolean,
            strikeProfileFn: (Vector3, Vector2, CarData) -> StrikeProfile,
            kickStrategy: KickStrategy): PrecisionPlan? {

        val myPosition = carData.position.flatten()
        var firstMomentInRange: GameTime? = null

        for (i in 0 until ballPath.slices.size) {
            val slice = ballPath.slices[i]
            val kickDirection = kickStrategy.getKickDirection(carData, slice.space) ?: continue

            val interceptModifier = kickDirection.scaledToMagnitude(-1 * (ArenaModel.BALL_RADIUS + 1.5))
            val spaceTime = SpaceTime(slice.space.plus(interceptModifier), slice.time)
            val interceptFlat = spaceTime.space.flatten()
            val toIntercept = interceptFlat - myPosition

            val strikeProfile = strikeProfileFn.invoke(spaceTime.space, kickDirection.flatten(), carData)

            // If it's a forward strike, it's safe to factor in orient duration now, which is good for efficiency.
            // Otherwise, defer until we have a route because angled strikes are tricky.
            val orientDuration = if (strikeProfile.isForward) AccelerationModel.getOrientDuration(carData, spaceTime.space) else Duration.ofMillis(0)
            val dts = acceleration.getMotionAfterDuration(Duration.between(carData.time, spaceTime.time) - orientDuration, strikeProfile) ?: return null

            val interceptDistance = toIntercept.magnitude()
            val rangeDeficiency = interceptDistance - dts.distance
            if (rangeDeficiency <= 0) {
                if (firstMomentInRange == null) {
                    firstMomentInRange = spaceTime.time
                }
                if (spatialPredicate.invoke(carData, spaceTime, strikeProfile)) {

                    val boostNeeded = boostNeededForAerial(spaceTime.space.z)
                    val spareTime = if (slice.time > firstMomentInRange) slice.time - firstMomentInRange else Duration.ofMillis(0)

                    val intercept = Intercept(
                            slice.space + interceptModifier,
                            slice.time,
                            boostNeeded,
                            strikeProfile,
                            acceleration,
                            spareTime,
                            slice,
                            dts)

                    val kickPlan = DirectedKickUtil.planKickFromIntercept(intercept, ballPath, carData, kickStrategy)
                            ?: return null // Also consider continuing the loop instead.
                    val steerPlan = kickPlan.launchPad.planRoute(carData, kickPlan.distancePlot)
//                    val steerPlan = CircleTurnUtil.getPlanForCircleTurn(carData, kickPlan.distancePlot, kickPlan.launchPad)
                    steerPlan.route.withPart(StrikeRoutePart(kickPlan.launchPad.position, kickPlan.intercept.space, kickPlan.intercept.strikeProfile))

                    val postRouteTime = Duration.between(carData.time, intercept.time) - steerPlan.route.duration

                    if (postRouteTime.millis >= -30 ||
                            // If it's an aerial, give some extra leeway. The route is currently not very good at judging how long
                            // an aerial will take.
                            strikeProfile.style == StrikeProfile.Style.AERIAL && postRouteTime.seconds > -1.0) {
                        return PrecisionPlan(kickPlan, steerPlan)
                    } else {
                        // It's not actually in range after we account for routing time.
                        // Walk back the first moment in range.
                        firstMomentInRange = null
                    }
                }
            }
        }

        // No slices in the ball slices were in range and satisfied the predicate
        return null
    }

    private fun boostNeededForAerial(height: Double) : Double {
        return if (height > StrikePlanner.NEEDS_AERIAL_THRESHOLD) StrikePlanner.BOOST_NEEDED_FOR_AERIAL else 0.0
    }

    private fun getTweenedSlice(ballPath: BallPath, currentSlice: BallSlice, currentSliceIndex: Int, currentShortfall: Double, previousShortfall: Double): BallSlice {

        if (currentSliceIndex == 0) {
            return currentSlice
        }

        var tweenPoint = 1.0
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
            launchMoment: GameTime): Intercept? {

        val myPosition = carData.position

        for (slice in ballPath.slices) {
            val intercept = SpaceTime(slice.space.plus(interceptModifier), slice.time)

            val timeSinceLaunch = Duration.between(launchMoment, carData.time)
            val duration = Duration.between(carData.time, slice.time)
            val aerialCourseCorrection = AerialMath.calculateAerialCourseCorrection(carData, intercept, timeSinceLaunch.seconds)
            val zComponent = aerialCourseCorrection.correctionDirection.z
            val desiredNoseAngle = Math.asin(zComponent)
            val currentNoseAngle = Math.asin(carData.orientation.noseVector.z)
            val currentAngleFactor = Math.min(1.0, 1 / duration.seconds)
            val averageNoseAngle = currentNoseAngle * currentAngleFactor + desiredNoseAngle * (1 - currentAngleFactor)

            val acceleration = AccelerationModel.simulateAirAcceleration(carData, duration, Math.cos(averageNoseAngle))

            // We're already in the air, so don't model any hang time.
            val strikeProfile =
                    if (timeSinceLaunch + duration  < MidairStrikeStep.MAX_TIME_FOR_AIR_DODGE && averageNoseAngle < .5)
                        CustomStrike(Duration.ZERO, Duration.ofMillis(150), 10.0, StrikeProfile.Style.AERIAL)
                    else
                        CustomStrike(Duration.ZERO, Duration.ZERO, 0.0, StrikeProfile.Style.AERIAL)

            val dts = acceleration.getMotionAfterDuration(Duration.between(carData.time, intercept.time), strikeProfile) ?: return null

            val interceptDistance = VectorUtil.flatDistance(myPosition, intercept.space)
            if (dts.distance > interceptDistance && AerialMath.isViableAerial(carData, intercept, timeSinceLaunch.seconds)) {
                return Intercept(
                        intercept.space,
                        intercept.time,
                        airBoost = 0.0,
                        strikeProfile = ChipStrike(),
                        distancePlot = acceleration,
                        spareTime = Duration.ofMillis(0),
                        ballSlice = slice,
                        accelSlice = dts)
            }

        }
        return null
    }
}
