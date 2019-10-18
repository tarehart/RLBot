package tarehart.rlbot.intercept.strike

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.steps.strikes.DirectedKickPlan
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

abstract class StrikeProfile {
    /**
     * The amount of time between strike initiation and any dodge.
     */
    abstract val preDodgeTime: Duration

    /**
     * The amount of time spent speeding up during the final stage
     */
    abstract val postDodgeTime: Duration

    /**
     * The amount of speed potentially gained over the course of the strike's final stage (generally after driving over and lining up)
     */
    abstract val speedBoost: Double

    abstract fun getPlan(car: CarData, intercept: SpaceTime): Plan?

    open fun getPlanFancy(car: CarData, kickPlan: DirectedKickPlan): Plan? {
        return getPlan(car, kickPlan.intercept.toSpaceTime())
    }

    abstract fun getPreKickWaypoint(car: CarData, intercept: Intercept, desiredKickForce: Vector3, expectedArrivalSpeed: Double): PreKickWaypoint?

    abstract val style: Style

    val strikeDuration: Duration
        get() = preDodgeTime + postDodgeTime

    abstract fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean

    open val isForward = true

    data class PostDodgeVelocity(val forwardMagnitude: Double, val sidewaysMagnitude: Double) {
        val speed = Vector2(forwardMagnitude, sidewaysMagnitude).magnitude()
    }

    /**
     * When we are doing either a side dodge or a diagonal dodge, the car will speed up in the forward and horizontal directions.
     */
    fun getPostDodgeVelocity(arrivalSpeed: Double): PostDodgeVelocity {

        // https://youtu.be/pX950bhGhJE?t=370
        val sidewaysImpulseMagnitude = ManeuverMath.DODGE_SPEED * (1 + 0.9 *  arrivalSpeed / AccelerationModel.SUPERSONIC_SPEED)

        val sidewaysComponent = if (isForward) 0.0 else sidewaysImpulseMagnitude
        val forwardComponent = if (style == Style.SIDE_HIT) 0.0 else ManeuverMath.DODGE_SPEED

        val tentativeFinalSpeed = Vector2(arrivalSpeed + forwardComponent, sidewaysComponent)
        val finalSpeed = tentativeFinalSpeed.magnitude()
        if (finalSpeed > AccelerationModel.SUPERSONIC_SPEED) {
            val scaledSpeed = tentativeFinalSpeed.scaledToMagnitude(AccelerationModel.SUPERSONIC_SPEED)
            return PostDodgeVelocity(scaledSpeed.x, scaledSpeed.y)
        }

        return PostDodgeVelocity(tentativeFinalSpeed.x, tentativeFinalSpeed.y)
    }

}
