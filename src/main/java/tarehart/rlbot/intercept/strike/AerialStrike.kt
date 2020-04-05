package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AerialChecklist
import tarehart.rlbot.intercept.AerialMath
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.Atan
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.KickStrategy
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.ManeuverMath
import kotlin.math.max

open class AerialStrike(height: Float, private val kickStrategy: KickStrategy?): StrikeProfile() {
    override val preDodgeTime = Duration.ofSeconds(AerialMath.timeToAir(height))

    override val speedBoost = 0F
    override val postDodgeTime = Duration.ZERO
    override val style = Style.AERIAL

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        val secondsTillIntercept = Duration.between(car.time, intercept.time).seconds

        if (car.boost > StrikePlanner.boostNeededForAerial(intercept.space.z)) {
            val tMinus = AerialStrike.getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept)
            return tMinus >= -0.1
        }
        return false
    }

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        if (isReadyForAerial(car, intercept)) {
            BotLog.println("Performing Aerial!", car.playerIndex)

            val groundDistance = car.position.flatten().distance(intercept.space.flatten())
            val radiansForTilt = Atan.atan2(intercept.space.z, groundDistance)

            val tiltBackSeconds = 0.1 + radiansForTilt * .1

            return performDoubleJumpAerial(tiltBackSeconds, kickStrategy, intercept)
        }

        return null
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Float): PreKickWaypoint? {

        val secondsTillIntercept = (intercept.time - car.time).seconds
        val flatPosition = car.position.flatten()
        val toIntercept = intercept.space.flatten() - flatPosition
        val distanceToIntercept = toIntercept.magnitude()
        val averageSpeedNeeded = distanceToIntercept / secondsTillIntercept
        val flatForce = desiredKickForce.flatten()
        val approachError = Vector2.angle(flatForce, toIntercept)

        val maxAerialApproachError = Math.PI / 4

        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        val momentOrNow = if (launchPadMoment.isBefore(car.time))
            car.time
        else
            launchPadMoment

        if (approachError < maxAerialApproachError) {

            val desiredSpeedAtLaunch = averageSpeedNeeded * 0.8F
            val orientSeconds = SteerUtil.getSteerPenaltySeconds(car, intercept.space)

            val motionAfterSpeedChange = ManeuverMath.getMotionAfterSpeedChange(
                    car.velocity.flatten().magnitude(), desiredSpeedAtLaunch, intercept.distancePlot) ?: return null

            return AnyFacingPreKickWaypoint(
                    position = car.position.flatten(),
                    idealFacing = toIntercept,
                    allowableFacingError = .2F,
                    expectedTime = car.time + Duration.ofSeconds(max(orientSeconds, motionAfterSpeedChange.time.seconds)),
                    expectedSpeed = desiredSpeedAtLaunch,
                    waitUntil = if (intercept.needsPatience) momentOrNow else null)
        }


        val lazyLaunchToIntercept = flatForce.rotateTowards(toIntercept, maxAerialApproachError)
        val facing = lazyLaunchToIntercept.normalized()

        return AnyFacingPreKickWaypoint(
                position = car.position.flatten(),
                idealFacing = facing,
                allowableFacingError = 1F,
                expectedTime = momentOrNow,
                expectedSpeed = averageSpeedNeeded,
                waitUntil = if (intercept.needsPatience) momentOrNow else null)
    }


    companion object {

        fun isReadyForAerial(car: CarData, intercept: SpaceTime): Boolean {

            val toTarget = intercept.space.flatten().minus(car.position.flatten())
            val velocityCorrectionRad = car.velocity.flatten().correctionAngle(toTarget)

            val checklist = AerialChecklist()
            StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
            checklist.timeForIgnition = true
            checklist.hasBoost = true
            checklist.notSkidding = !ManeuverMath.isSkidding(car)
            checklist.linedUp = car.velocity.magnitude() < 5 || Math.abs(velocityCorrectionRad) < 0.1
            if (!checklist.readyToLaunch()) {
                return false
            }

            val avgSpeedNeeded = car.position.flatten().distance(intercept.space.flatten()) / (intercept.time - car.time).seconds
            val speedRatio = car.velocity.magnitude() / avgSpeedNeeded
            val timeToAirRatio = AerialMath.timeToAir(intercept.space.z) / (intercept.time - car.time).seconds

            if (speedRatio < 0.6 || timeToAirRatio < 0.6) {
                return false
            }

            val courseCorrection = AerialMath.calculateAerialCourseCorrection(
                    CarSlice(car), intercept, true, 0F)
            val accelRatio = courseCorrection.averageAccelerationRequired / AerialMath.BOOST_ACCEL_IN_AIR

            if (accelRatio < 0.4) {
                return false
            }

            return true
        }

        fun getAerialLaunchCountdown(height: Float, secondsTillIntercept: Float): Float {
            val expectedAerialSeconds = AerialMath.timeToAir(height)
            return secondsTillIntercept - expectedAerialSeconds
        }

        fun performDoubleJumpAerial(tiltBackSeconds: Double, kickStrategy: KickStrategy?, intercept: SpaceTime): Plan {
            val tiltBackDuration = Duration.ofSeconds(tiltBackSeconds)

            return Plan()
                    .withStep(BlindStep(tiltBackDuration, AgentOutput().withJump(true).withPitch(1.0)))
                    .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput()
                            .withPitch(1.0)
                            .withBoost(true)
                    ))
                    .withStep(BlindStep(Duration.ofSeconds(.05), AgentOutput()
                            .withBoost(true)
                            .withJump(true)
                    ))
                    .withStep(MidairStrikeStep(tiltBackDuration, hasJump = false, kickStrategy = kickStrategy, initialIntercept = intercept))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }
    }
}
