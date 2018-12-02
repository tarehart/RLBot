package tarehart.rlbot.steps.strikes

import rlbot.render.Renderer
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.math.Triangle
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.routing.waypoint.StrictPreKickWaypoint
import tarehart.rlbot.tuning.ManeuverMath
import java.awt.Color

object DirectedKickUtil {
    private val BALL_VELOCITY_INFLUENCE = .2

    fun planKickFromIntercept(intercept: Intercept, ballPath: BallPath, car: CarData, kickStrategy: KickStrategy): DirectedKickPlan? {

        val ballAtIntercept = intercept.ballSlice
        val secondsTillIntercept = (intercept.time - car.time).seconds
        val flatPosition = car.position.flatten()
        val toIntercept = intercept.space.flatten() - flatPosition
        val distanceToIntercept = toIntercept.magnitude()
        val averageSpeedNeeded = distanceToIntercept / secondsTillIntercept
        val currentSpeed = car.velocity.flatten().magnitude()
        val anticipatedSpeed = if (intercept.spareTime.millis > 0) Math.max(currentSpeed, averageSpeedNeeded) else intercept.accelSlice.speed
        val closenessRatio = Math.min(1.0, 1 / secondsTillIntercept)
        val arrivalSpeed = closenessRatio * currentSpeed + (1 - closenessRatio) * anticipatedSpeed
        val interceptModifier = intercept.space - intercept.ballSlice.space

        var kickDirection: Vector3
        val easyKickAllowed: Boolean
        var plannedKickForce = Vector3() // This empty vector will never be used, but the compiler hasn't noticed.
        var desiredBallVelocity = Vector3()

        val impactSpeed = if (intercept.strikeProfile.style == StrikeProfile.Style.SIDE_HIT) ManeuverMath.DODGE_SPEED else Math.max(10.0, arrivalSpeed)

        if (intercept.strikeProfile.isForward) {

            val easyForce: Vector3 = (ballAtIntercept.space - car.position).scaledToMagnitude(impactSpeed)
            val easyKick = bump(ballAtIntercept.velocity, easyForce)
            kickDirection = kickStrategy.getKickDirection(car, ballAtIntercept.space, easyKick) ?: return null
            easyKickAllowed = easyKick.x == kickDirection.x && easyKick.y == kickDirection.y
            if (easyKickAllowed) {
                // The kick strategy is fine with the easy kick.
                plannedKickForce = easyForce
                desiredBallVelocity = easyKick
            }
        } else {
            easyKickAllowed = false
        }

        if (!easyKickAllowed) {

            // TODO: this is a rough approximation.
            kickDirection = kickStrategy.getKickDirection(car, ballAtIntercept.space) ?: return null
            val orthogonal = VectorUtil.orthogonal(kickDirection.flatten())
            val transverseBallVelocity = VectorUtil.project(ballAtIntercept.velocity.flatten(), orthogonal)
            desiredBallVelocity = kickDirection.normaliseCopy().scaled(impactSpeed * 2)
            plannedKickForce = Vector3(
                    desiredBallVelocity.x - transverseBallVelocity.x * BALL_VELOCITY_INFLUENCE,
                    desiredBallVelocity.y - transverseBallVelocity.y * BALL_VELOCITY_INFLUENCE,
                    desiredBallVelocity.z)
        }

        val launchPad = intercept.strikeProfile.getPreKickWaypoint(car, intercept, plannedKickForce, arrivalSpeed) ?: return null

        return DirectedKickPlan(
                interceptModifier = interceptModifier,
                ballPath = ballPath,
                distancePlot = intercept.distancePlot,
                intercept = intercept,
                plannedKickForce = plannedKickForce,
                desiredBallVelocity = desiredBallVelocity,
                launchPad = launchPad,
                easyKickAllowed = easyKickAllowed
        )
    }

    /**
     * The car is approaching the intercept. If it drives there in a straight line, we consider that to be the
     * approach vector. If the approach vector is perfectly aligned with the intended kick force, that's easy--
     * no deviation. This method measures the deviation in radians. Can return positive or negative.
     */
    fun getEstimatedApproachDeviationFromKickForce(car: CarData, kickLocation: Vector2, desiredForce: Vector2): Double {
        val estimatedApproach = ManeuverMath.estimateApproachVector(PositionFacing(car), kickLocation)
        return estimatedApproach.correctionAngle(desiredForce)
    }

    fun getStandardWaypoint(launchPosition: Vector2, facing: Vector2, intercept: Intercept): PreKickWaypoint {
        val launchPad: PreKickWaypoint

        // Time is chosen with a bias toward hurrying
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        launchPad = StrictPreKickWaypoint(
                position = launchPosition,
                facing = facing,
                expectedTime = launchPadMoment,
                waitUntil = if (intercept.spareTime.millis > 0) launchPadMoment else null
        )

        return launchPad
    }

    fun getAnyFacingWaypoint(launchPosition: Vector2, intercept: Intercept): PreKickWaypoint {
        val launchPad: PreKickWaypoint

        // Time is chosen with a bias toward hurrying
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        launchPad = AnyFacingPreKickWaypoint(
                position = launchPosition,
                expectedTime = launchPadMoment,
                waitUntil = if (intercept.spareTime.millis > 0) launchPadMoment else null
        )

        return launchPad
    }

    /**
     * Definitions:
     * hopPosition: Where the car first jumps off the ground
     * dodgePosition: The place in midair where the car initiates a dodge with the second jump
     * strikeTravel: The distance between the dodgePosition to the car's position at the moment of ball contact
     */
    fun getAngledWaypoint(intercept: Intercept, arrivalSpeed: Double, approachVsKickForceAngle:Double,
                          carPosition: Vector2, carPositionAtContact: Vector2, renderer: Renderer): PreKickWaypoint? {

        val postDodgeVelocity = intercept.strikeProfile.getPostDodgeVelocity(arrivalSpeed)

        val deflectionAngle = Math.atan2(postDodgeVelocity.sidewaysMagnitude, postDodgeVelocity.forwardMagnitude) * Math.signum(approachVsKickForceAngle)

        if (Math.abs(approachVsKickForceAngle - deflectionAngle) > Math.PI * .45) {
            return null
        }

        val strikeTravel = intercept.strikeProfile.postDodgeTime.seconds * postDodgeVelocity.speed

        val dodgePosition = dodgePosition(carPosition, carPositionAtContact, deflectionAngle, strikeTravel) ?: return null



        // Time is chosen with a bias toward hurrying
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration

        val dodgePositionToHop = (carPosition - dodgePosition).scaledToMagnitude(intercept.strikeProfile.preDodgeTime.seconds * arrivalSpeed)
        val hopPosition = dodgePosition + dodgePositionToHop

        renderer.drawLine3d(Color.ORANGE, dodgePosition.withZ(intercept.space.z).toRlbot(), carPositionAtContact.withZ(intercept.space.z).toRlbot())
        renderer.drawLine3d(Color.YELLOW, hopPosition.withZ(ManeuverMath.BASE_CAR_Z).toRlbot(), dodgePosition.withZ(intercept.space.z).toRlbot())

        return AnyFacingPreKickWaypoint(
                position = hopPosition,
                expectedTime = launchPadMoment,
                waitUntil = if (intercept.spareTime.millis > 0) launchPadMoment else null
        )
    }

    private fun dodgePosition(carPosition: Vector2, carAtContact: Vector2, dodgeDeflectionAngle: Double, dodgeTravel: Double): Vector2? {

        val toContact = carAtContact - carPosition
        val triangle = Triangle.sideSideAngle(toContact.magnitude(), dodgeTravel, Math.PI - Math.abs(dodgeDeflectionAngle)) ?: return null

        // We want the location of pt A as the launch position.
        // carPosition is at B
        // carAtContact is at C

        // Let's find the absolute angle of side B, by getting the absolute angle of side A and then adding angleC to it.
        val sideAAngle = Math.atan2(toContact.y, toContact.x)
        val sideBAngle = sideAAngle + triangle.angleB * Math.signum(-dodgeDeflectionAngle) // TODO: is this inverted?
        val toDodge = Vector2(Math.cos(sideBAngle), Math.sin(sideBAngle)).scaled(triangle.sideC)
        return carPosition + toDodge

    }

    /**
     * https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
     */
    private fun reflect(incident: Vector3, normal: Vector3): Vector3 {

        val normalized = normal.normaliseCopy()
        return incident - normalized.scaled(2 * incident.dotProduct(normalized))
    }

    private fun bump(incident: Vector3, movingWall: Vector3): Vector3 {
        // Move into reference frame of moving wall
        val incidentAccordingToWall = incident - movingWall
        val reflectionAccordingToWall = reflect(incidentAccordingToWall, movingWall)
        return reflectionAccordingToWall + movingWall
    }
}
