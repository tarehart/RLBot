package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AerialChecklist
import tarehart.rlbot.intercept.AerialMath
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.RoutePlanner
import tarehart.rlbot.routing.waypoint.FacingAndSpeedPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.routing.waypoint.StrictPreKickWaypoint
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.DirectedKickPlan
import tarehart.rlbot.steps.strikes.KickStrategy
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.ManeuverMath

open class AerialStrike(height: Double, private val kickStrategy: KickStrategy?): StrikeProfile() {
    override val preDodgeTime = Duration.ofSeconds(AerialMath.timeToAir(height))

    private val canDodge = preDodgeTime < MidairStrikeStep.MAX_TIME_FOR_AIR_DODGE
    override val speedBoost = if (canDodge) 10.0 else 0.0
    override val postDodgeTime = if (canDodge) Duration.ofMillis(250) else Duration.ZERO
    override val style = Style.AERIAL

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return StrikePlanner.isVerticallyAccessible(car, intercept)
    }

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        if (isReadyForAerial(car, intercept)) {
            BotLog.println("Performing Aerial!", car.playerIndex)

            val groundDistance = car.position.flatten().distance(intercept.space.flatten())
            val radiansForTilt = Math.atan2(intercept.space.z, groundDistance) + UPWARD_VELOCITY_MAINTENANCE_ANGLE

            val tiltBackSeconds = 0.2 + radiansForTilt * .1

            return if (Duration.between(car.time, intercept.time).seconds > 1.5 && intercept.space.z > 10) {
                performDoubleJumpAerial(tiltBackSeconds * .8, kickStrategy)
            } else performAerial(tiltBackSeconds, kickStrategy)
        }

        return null
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Double): PreKickWaypoint? {

        val secondsTillIntercept = (intercept.time - car.time).seconds
        val flatPosition = car.position.flatten()
        val toIntercept = intercept.space.flatten() - flatPosition
        val distanceToIntercept = toIntercept.magnitude()
        val averageSpeedNeeded = distanceToIntercept / secondsTillIntercept
        val flatForce = desiredKickForce.flatten()
        val approachError = Vector2.angle(flatForce, toIntercept)

        val speedForBackoff = Math.max(car.velocity.flatten().magnitude(), averageSpeedNeeded)
        val idealLaunchToIntercept = flatForce.scaledToMagnitude(strikeDuration.seconds * speedForBackoff)
        val maxAerialApproachError = Math.PI / 4

        if (approachError < maxAerialApproachError) {

            val desiredSpeedAtLaunch = averageSpeedNeeded * 0.9
            val orientSeconds = FacingAndSpeedPreKickWaypoint.getOrientDuration(car.orientation.noseVector.flatten(), toIntercept).seconds

            val motionAfterSpeedChange = RoutePlanner.getMotionAfterSpeedChange(
                    car.velocity.flatten().magnitude(), desiredSpeedAtLaunch, intercept.distancePlot) ?: return null

            return FacingAndSpeedPreKickWaypoint(
                    position = car.position.flatten(),
                    facing = toIntercept,
                    expectedTime = car.time + Duration.ofSeconds(Math.max(orientSeconds, motionAfterSpeedChange.time.seconds)),
                    speed = desiredSpeedAtLaunch)
        }

        val lazyLaunchToIntercept = idealLaunchToIntercept.rotateTowards(toIntercept, maxAerialApproachError)
        val launchPosition = intercept.space.flatten() - lazyLaunchToIntercept
        val facing = lazyLaunchToIntercept.normalized()
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        val momentOrNow = if (launchPadMoment.isBefore(car.time))
            car.time
        else
            launchPadMoment

        return StrictPreKickWaypoint(
                position = launchPosition,
                facing = facing,
                expectedTime = momentOrNow,
                expectedSpeed = averageSpeedNeeded,
                waitUntil = if (intercept.needsPatience) momentOrNow else null)
    }


    companion object {
        private const val UPWARD_VELOCITY_MAINTENANCE_ANGLE = .25

        fun isReadyForAerial(car: CarData, intercept: SpaceTime): Boolean {

            val checklist = AerialChecklist()
            StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
            checklist.timeForIgnition = true
            checklist.hasBoost = true
            checklist.notSkidding = !ManeuverMath.isSkidding(car)
            if (!checklist.readyToLaunch()) {
                BotLog.println("Not aerialing yet: $checklist", car.playerIndex)
                return false
            }

            val avgSpeedNeeded = car.position.flatten().distance(intercept.space.flatten()) / (intercept.time - car.time).seconds
            val speedRatio = car.velocity.magnitude() / avgSpeedNeeded

            if (speedRatio < 0.75) {
                BotLog.println("Not aerialing yet: Have only sped up to $speedRatio of the average speed needed", car.playerIndex)
                return false
            }

            val courseCorrection = AerialMath.calculateAerialCourseCorrection(
                    CarSlice(car), intercept, true, 0.0, false)
            val accelRatio = courseCorrection.averageAccelerationRequired / AerialMath.BOOST_ACCEL_IN_AIR

            if (accelRatio < 0.4) {
                BotLog.println("Not aerialing yet: Only need to boost $accelRatio of the time", car.playerIndex)
                return false
            }

            return true
        }

        fun getAerialLaunchCountdown(height: Double, secondsTillIntercept: Double): Double {
            val expectedAerialSeconds = AerialMath.timeToAir(height)
            return secondsTillIntercept - expectedAerialSeconds
        }

        fun performAerial(tiltBackSeconds: Double, kickStrategy: KickStrategy?): Plan {
            val tiltBackDuration = Duration.ofSeconds(tiltBackSeconds)

            return Plan()
                    .withStep(BlindStep(tiltBackDuration, AgentOutput().withJump(true).withPitch(1.0)))
                    .withStep(MidairStrikeStep(tiltBackDuration, kickStrategy = kickStrategy))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }

        fun performDoubleJumpAerial(tiltBackSeconds: Double, kickStrategy: KickStrategy?): Plan {
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
                    .withStep(MidairStrikeStep(tiltBackDuration, hasJump = false, kickStrategy = kickStrategy))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }
    }
}