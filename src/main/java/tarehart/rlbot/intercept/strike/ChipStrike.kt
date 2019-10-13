package tarehart.rlbot.intercept.strike

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.routing.waypoint.StrictPreKickWaypoint
import tarehart.rlbot.steps.strikes.DirectedKickUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class ChipStrike: StrikeProfile() {

    override val preDodgeTime = Duration.ZERO
    override val speedBoost = 0.0
    override val postDodgeTime = Duration.ZERO
    override val style = Style.CHIP

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z <= MAX_HEIGHT_OF_BALL_FOR_CHIP
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Double): PreKickWaypoint? {

        val estimatedApproachDeviationFromKickForce = DirectedKickUtil.getEstimatedApproachDeviationFromKickForce(
                car, intercept.space.flatten(), desiredKickForce.flatten())

        val carCornerSpacing = Math.abs(estimatedApproachDeviationFromKickForce) * 1.1

        val distanceBetweenCentersAtContact = desiredKickForce.flatten().scaledToMagnitude(3.2 + carCornerSpacing)

        val launchPosition = intercept.ballSlice.space.flatten() - distanceBetweenCentersAtContact
        val carToLaunch = launchPosition - car.position.flatten()

        // Time is chosen with a bias toward hurrying
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        return StrictPreKickWaypoint(
                position = launchPosition,
                facing = desiredKickForce.flatten().rotateTowards(carToLaunch, Math.PI * .15),
                expectedTime = launchPadMoment,
                waitUntil = if (intercept.needsPatience) launchPadMoment else null
        )
    }

    companion object {
        const val MAX_HEIGHT_OF_BALL_FOR_CHIP = ArenaModel.BALL_RADIUS + ManeuverMath.BASE_CAR_Z + 0.1
    }
}
