package tarehart.rlbot.intercept.strike

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AerialChecklist
import tarehart.rlbot.intercept.AerialMath
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.StrikePlanner
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.routing.waypoint.StrictPreKickWaypoint
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.landing.LandGracefullyStep
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.ManeuverMath

open class AerialStrike(height: Double): StrikeProfile() {
    override val preDodgeTime = Duration.ofSeconds(AerialMath.timeToAir(height))

    private val canDodge = preDodgeTime < MidairStrikeStep.MAX_TIME_FOR_AIR_DODGE
    override val speedBoost = if (canDodge) 10.0 else 0.0
    override val postDodgeTime = if (canDodge) Duration.ofMillis(250) else Duration.ZERO
    override val style = Style.AERIAL

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return StrikePlanner.isVerticallyAccessible(car, intercept)
    }

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        val checklist = checkAerialReadiness(car, intercept)
        if (checklist.readyToLaunch()) {
            BotLog.println("Performing Aerial!", car.playerIndex)

            val groundDistance = car.position.flatten().distance(intercept.space.flatten())
            val radiansForTilt = Math.atan2(intercept.space.z, groundDistance) + UPWARD_VELOCITY_MAINTENANCE_ANGLE

            val tiltBackSeconds = 0.2 + radiansForTilt * .1

            return if (Duration.between(car.time, intercept.time).seconds > 1.5 && intercept.space.z > 10) {
                performDoubleJumpAerial(tiltBackSeconds * .8)
            } else performAerial(0.1)
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

        val idealLaunchToIntercept = flatForce.scaledToMagnitude(strikeDuration.seconds * averageSpeedNeeded)
        var lazyLaunchToIntercept = idealLaunchToIntercept.rotateTowards(toIntercept, Math.PI / 4)
        val lazyLaunchDistance = lazyLaunchToIntercept.magnitude()
        var useStrict = true
        if (lazyLaunchDistance > distanceToIntercept && distanceToIntercept / lazyLaunchDistance > 0.8) {
            val alignmentError = 10 * Vector2.angle(lazyLaunchToIntercept, car.orientation.noseVector.flatten())
            lazyLaunchToIntercept = lazyLaunchToIntercept.scaledToMagnitude(distanceToIntercept - alignmentError)
            useStrict = false
        }
        val launchPosition = intercept.space.flatten() - lazyLaunchToIntercept
        val facing = lazyLaunchToIntercept.normalized()
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        val momentOrNow = if (launchPadMoment.isBefore(car.time)) car.time else launchPadMoment
        return if (useStrict)
            StrictPreKickWaypoint(
                position = launchPosition,
                facing = facing,
                expectedTime = momentOrNow,
                waitUntil = if (intercept.spareTime.millis > 0) momentOrNow else null)
        else
            AnyFacingPreKickWaypoint(
                position = launchPosition,
                expectedTime = momentOrNow,
                waitUntil = if (intercept.spareTime.millis > 0) momentOrNow else null)
    }


    companion object {
        private const val UPWARD_VELOCITY_MAINTENANCE_ANGLE = .25

        fun checkAerialReadiness(car: CarData, intercept: SpaceTime): AerialChecklist {

            val checklist = AerialChecklist()
            StrikePlanner.checkLaunchReadiness(checklist, car, intercept)
            val secondsTillIntercept = Duration.between(car.time, intercept.time).seconds
            val tMinus = getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept)
            checklist.timeForIgnition = tMinus < 0.1
            checklist.notSkidding = !ManeuverMath.isSkidding(car)
            checklist.hasBoost = car.boost >= StrikePlanner.BOOST_NEEDED_FOR_AERIAL

            return checklist
        }

        fun getAerialLaunchCountdown(height: Double, secondsTillIntercept: Double): Double {
            val expectedAerialSeconds = AerialMath.timeToAir(height)
            return secondsTillIntercept - expectedAerialSeconds
        }

        fun performAerial(tiltBackSeconds: Double): Plan {
            val tiltBackDuration = Duration.ofSeconds(tiltBackSeconds)

            return Plan()
                    .withStep(BlindStep(tiltBackDuration, AgentOutput().withJump(true).withPitch(1.0)))
                    .withStep(MidairStrikeStep(tiltBackDuration))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }

        fun performDoubleJumpAerial(tiltBackSeconds: Double): Plan {
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
                    .withStep(MidairStrikeStep(tiltBackDuration, hasJump = false))
                    .withStep(LandGracefullyStep(LandGracefullyStep.FACE_BALL))
        }
    }
}