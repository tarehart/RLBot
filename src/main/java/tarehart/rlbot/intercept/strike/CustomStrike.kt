package tarehart.rlbot.intercept.strike

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.time.Duration

class CustomStrike(
        override val preDodgeTime: Duration,
        override val postDodgeTime: Duration,
        override val speedBoost: Double,
        override val style: Style): StrikeProfile() {

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        throw NotImplementedError("You should not be calling this on a custom strike!")
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        throw NotImplementedError("You should not be calling this on a custom strike!")
    }

    override fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Double): PreKickWaypoint? {
        throw NotImplementedError("You should not be calling this on a custom strike!")
    }
}