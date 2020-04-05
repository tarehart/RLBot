package tarehart.rlbot.steps.strikes

import rlbot.render.Renderer
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.Atan
import tarehart.rlbot.math.Triangle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

object DirectedKickUtil {

    fun planKickFromIntercept(intercept: Intercept, ballPath: BallPath, car: CarData, kickStrategy: KickStrategy): DirectedKickPlan? {

        val idealDirection = kickStrategy.getKickDirection(car, intercept.space) ?:
        return null

        val arrivalHeight = intercept.ballSlice.space.z - ArenaModel.BALL_RADIUS + ManeuverMath.BASE_CAR_Z

        val chipOption = BallPhysics.computeBestChipOption(car.position, intercept.accelSlice.speed,
                intercept.ballSlice, car.hitbox, idealDirection, arrivalHeight)

        if (chipOption == null) {
            BotLog.println("Could not compute chip option", car.playerIndex)
            return null
        }

        val plannedKickForce1 = (intercept.ballSlice.space - chipOption.impactPoint).scaledToMagnitude(chipOption.velocity.magnitude())
        val launchPad = intercept.strikeProfile.getPreKickWaypoint(car, intercept, plannedKickForce1, intercept.accelSlice.speed) ?: return null

        return DirectedKickPlan(intercept, ballPath, intercept.distancePlot, chipOption.velocity, plannedKickForce1, launchPad)
    }

    /**
     * The car is approaching the intercept. If it drives there in a straight line, we consider that to be the
     * approach vector. If the approach vector is perfectly aligned with the intended kick force, that's easy--
     * no deviation. This method measures the deviation in radians. Can return positive or negative.
     */
    fun getEstimatedApproachDeviationFromKickForce(car: CarData, kickLocation: Vector2, desiredForce: Vector2): Float {
        val estimatedApproach = ManeuverMath.estimateApproachVector(PositionFacing(car), kickLocation)
        return estimatedApproach.correctionAngle(desiredForce)
    }

    @Deprecated("This originally vended strict waypoints, and may not be valid anymore in many cases")
    fun getStandardWaypoint(launchPosition: Vector2, facing: Vector2, intercept: Intercept): PreKickWaypoint {
        val launchPad: PreKickWaypoint

        // Time is chosen with a bias toward hurrying
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        launchPad = AnyFacingPreKickWaypoint(
                position = launchPosition,
                idealFacing = facing,
                allowableFacingError = 1F,
                expectedTime = launchPadMoment,
                waitUntil = if (intercept.spatialPredicateFailurePeriod.millis > 0) launchPadMoment else null
        )

        return launchPad
    }

    /**
     * Definitions:
     * hopPosition: Where the car first jumps off the ground
     * dodgePosition: The place in midair where the car initiates a dodge with the second jump
     * strikeTravel: The distance between the dodgePosition to the car's position at the moment of ball contact
     */
    fun getAngledWaypoint(intercept: Intercept, arrivalSpeed: Float, approachVsKickForceAngle:Float,
                          carPosition: Vector2, carPositionAtContact: Vector2, shallowAngleThreshold: Float, renderer: Renderer): PreKickWaypoint? {

        val postDodgeVelocity = intercept.strikeProfile.getPostDodgeVelocity(arrivalSpeed)

        val strikeTravel = intercept.strikeProfile.postDodgeTime.seconds * postDodgeVelocity.speed
        val deflectionAngle = Atan.atan2(postDodgeVelocity.sidewaysMagnitude, postDodgeVelocity.forwardMagnitude) * sign(approachVsKickForceAngle)
        // Time is chosen with a bias toward hurrying
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration

        val dodgePosition = dodgePosition(carPosition, carPositionAtContact, deflectionAngle, strikeTravel) ?: return null
        val dodgePositionToHop = (carPosition - dodgePosition).scaledToMagnitude(intercept.strikeProfile.preDodgeTime.seconds * arrivalSpeed)
        val hopPosition = dodgePosition + dodgePositionToHop

        renderer.drawLine3d(Color.ORANGE, dodgePosition.withZ(intercept.space.z), carPositionAtContact.withZ(intercept.space.z))
        renderer.drawLine3d(Color.YELLOW, hopPosition.withZ(ManeuverMath.BASE_CAR_Z), dodgePosition.withZ(intercept.space.z))

        return AnyFacingPreKickWaypoint(
                position = hopPosition,
                idealFacing = dodgePosition - carPosition,
                allowableFacingError = Math.PI.toFloat() / 8,
                expectedTime = launchPadMoment,
                waitUntil = if (intercept.needsPatience) launchPadMoment else null
        )
    }

    private fun dodgePosition(carPosition: Vector2, carAtContact: Vector2, dodgeDeflectionAngle: Float, dodgeTravel: Float): Vector2? {

        val toContact = carAtContact - carPosition
        val toContactMagnitude = toContact.magnitude()
        if (dodgeTravel > toContactMagnitude) {
            return null  // This will be an impossible triangle
        }
        val triangle = Triangle.sideSideAngle(toContactMagnitude, dodgeTravel, Math.PI.toFloat() - abs(dodgeDeflectionAngle)) ?: return null

        // We want the location of pt A as the launch position.
        // carPosition is at B
        // carAtContact is at C

        // Let's find the absolute angle of side B, by getting the absolute angle of side A and then adding angleC to it.
        val sideAAngle = Atan.atan2(toContact.y, toContact.x)
        val sideBAngle = sideAAngle + triangle.angleB * sign(-dodgeDeflectionAngle) // TODO: is this inverted?
        val toDodge = Vector2(cos(sideBAngle), sin(sideBAngle)).scaled(triangle.sideC)
        return carPosition + toDodge

    }
}
