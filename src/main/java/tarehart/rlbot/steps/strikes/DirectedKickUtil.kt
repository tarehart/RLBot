package tarehart.rlbot.steps.strikes

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.Triangle
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.routing.PositionFacing
import tarehart.rlbot.routing.waypoint.AnyFacingPreKickWaypoint
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.routing.waypoint.StrictPreKickWaypoint
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.ManeuverMath

object DirectedKickUtil {
    private val BALL_VELOCITY_INFLUENCE = .2

    fun planKickFromIntercept(intercept: Intercept, ballPath: BallPath, car: CarData, kickStrategy: KickStrategy, estimatedApproach: Vector2): DirectedKickPlan? {

        //, distancePlot: DistancePlot, interceptModifier: Vector3, kickStrategy: KickStrategy, ballPath: BallPath

        val ballAtIntercept = intercept.ballSlice
        val secondsTillIntercept = (intercept.time - car.time).seconds
        val flatPosition = car.position.flatten()
        val toIntercept = intercept.space.flatten() - flatPosition
        val averageSpeedNeeded = toIntercept.magnitude() / secondsTillIntercept
        val arrivalSpeed = if (intercept.spareTime.millis > 0) averageSpeedNeeded else intercept.accelSlice.speed
        val impactSpeed: Double
        val interceptModifier = intercept.space - intercept.ballSlice.space

        val easyForce: Vector3
        if (intercept.strikeProfile.style == StrikeProfile.Style.SIDE_HIT) {
            impactSpeed = ManeuverMath.DODGE_SPEED
            val carToIntercept = (intercept.space - car.position).flatten()
            val (x, y) = VectorUtil.orthogonal(carToIntercept) { v -> v.dotProduct(interceptModifier.flatten()) < 0 }
            easyForce = Vector3(x, y, 0.0).scaledToMagnitude(impactSpeed)
        } else {
            // TODO: do something special for diagonal
            impactSpeed = arrivalSpeed
            easyForce = (ballAtIntercept.space - car.position).scaledToMagnitude(impactSpeed)
        }

        val easyKick = bump(ballAtIntercept.velocity, easyForce)
        val kickDirection = kickStrategy.getKickDirection(car, ballAtIntercept.space, easyKick) ?: return null

        val plannedKickForce: Vector3
        val desiredBallVelocity: Vector3

        val easyKickAllowed = easyKick.x == kickDirection.x && easyKick.y == kickDirection.y;

        if (easyKickAllowed) {
            // The kick strategy is fine with the easy kick.
            plannedKickForce = easyForce
            desiredBallVelocity = easyKick
        } else {

            // TODO: this is a rough approximation.
            val orthogonal = VectorUtil.orthogonal(kickDirection.flatten())
            val transverseBallVelocity = VectorUtil.project(ballAtIntercept.velocity.flatten(), orthogonal)
            desiredBallVelocity = kickDirection.normaliseCopy().scaled(impactSpeed + transverseBallVelocity.magnitude() * .7)
            plannedKickForce = Vector3(
                    desiredBallVelocity.x - transverseBallVelocity.x * BALL_VELOCITY_INFLUENCE,
                    desiredBallVelocity.y - transverseBallVelocity.y * BALL_VELOCITY_INFLUENCE,
                    desiredBallVelocity.z)
        }

        val toInterceptNorm = toIntercept.normalized()
        val flatForce = plannedKickForce.flatten()
        val toKickForce = toInterceptNorm.correctionAngle(flatForce)
        val estimatedApproachToKickForce = estimatedApproach.correctionAngle(flatForce) // TODO: Use this in more situations
        val ballPosition = ballAtIntercept.space.flatten()


        val launchPosition: Vector2
        val facing: Vector2

        val strikeDuration = intercept.strikeProfile.strikeDuration
        val launchPad: PreKickWaypoint

        when (intercept.strikeProfile.style) {
            StrikeProfile.Style.CHIP -> {
                facing = VectorUtil.rotateVector(toInterceptNorm, toKickForce * .5)
                launchPosition = intercept.space.flatten()
                launchPad = getStandardWaypoint(car, launchPosition, facing, intercept)
            }
            StrikeProfile.Style.DIAGONAL_HIT -> {

                val angled = getAngledWaypoint(intercept, arrivalSpeed, flatForce, estimatedApproachToKickForce, flatPosition)
                if (angled == null) {
                    BotLog.println("Failed to calculate diagonal waypoint", car.playerIndex)
                    return null
                }
                launchPad = angled
            }
            StrikeProfile.Style.SIDE_HIT -> {
//                val angled = getAngledWaypoint(intercept, arrivalSpeed, flatForce, estimatedApproachToKickForce, flatPosition)
//                if (angled == null) {
//                    BotLog.println("Failed to calculate side hit waypoint", car.playerIndex)
//                    return null
//                }
//                launchPad = angled

                facing = VectorUtil.rotateVector(flatForce, -Math.signum(toKickForce) * Math.PI / 2)
                launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(2.0) -
                        facing.scaledToMagnitude(intercept.strikeProfile.strikeDuration.seconds * arrivalSpeed)
                // TODO: consider separating out the hop position and the dodge position
                launchPad = getStandardWaypoint(car, launchPosition, facing, intercept)
            }
            StrikeProfile.Style.FLIP_HIT -> {
                facing = flatForce.normalized()
                launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(intercept.strikeProfile.strikeDuration.seconds * arrivalSpeed)
                launchPad = getStandardWaypoint(car, launchPosition, facing, intercept)
            }
            StrikeProfile.Style.JUMP_HIT -> {
                facing = flatForce.normalized()
                launchPosition = ballPosition - flatForce.scaledToMagnitude(intercept.strikeProfile.strikeDuration.seconds * arrivalSpeed)
                launchPad = getStandardWaypoint(car, launchPosition, facing, intercept)
            }
            StrikeProfile.Style.AERIAL -> {

                facing = toInterceptNorm.rotateTowards(flatForce, Math.PI / 6)
                launchPosition = ballPosition - facing.scaledToMagnitude(strikeDuration.seconds * averageSpeedNeeded)
                launchPad = getStandardWaypoint(car, launchPosition, facing, intercept)
            }
        }


        val kickPlan = DirectedKickPlan(
                interceptModifier = interceptModifier,
                ballPath = ballPath,
                distancePlot = intercept.distancePlot,
                intercept = intercept,
                plannedKickForce = plannedKickForce,
                desiredBallVelocity = desiredBallVelocity,
                launchPad = launchPad,
                easyKickAllowed = easyKickAllowed
        )

        return kickPlan
    }

    private fun getStandardWaypoint(car: CarData, launchPosition: Vector2, facing: Vector2, intercept: Intercept): PreKickWaypoint {
        val launchPad: PreKickWaypoint
        if (ManeuverMath.hasBlownPast(car, launchPosition, facing)) {
            val currentPositionFacing = PositionFacing(car)
            launchPad = StrictPreKickWaypoint(
                    position = currentPositionFacing.position,
                    facing = currentPositionFacing.facing,
                    expectedTime = car.time
            )
        } else {
            // Time is chosen with a bias toward hurrying
            val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
            launchPad = StrictPreKickWaypoint(
                    position = launchPosition,
                    facing = facing,
                    expectedTime = launchPadMoment,
                    waitUntil = if (intercept.spareTime.millis > 0) launchPadMoment else null
            )
        }
        return launchPad
    }

    private fun getAngledWaypoint(intercept: Intercept, arrivalSpeed: Double, kickForce: Vector2, approachVsKickForceAngle:Double, carPosition: Vector2): PreKickWaypoint? {
        val strikeTravel = intercept.strikeProfile.dodgeSeconds * arrivalSpeed

        val carPositionAtContact = intercept.ballSlice.space.flatten() - kickForce.scaledToMagnitude(2.5)

        val speedupResult = getSpeedupResultForAngled(intercept.strikeProfile, arrivalSpeed)

        val deflectionAngle = Math.atan2(speedupResult.sidewaysMagnitude, speedupResult.forwardMagnitude) * Math.signum(approachVsKickForceAngle)

        val dodgePosition = dodgePosition(carPosition, carPositionAtContact, deflectionAngle, strikeTravel) ?: return null

        val dodgePositionToHop = (carPosition - dodgePosition).scaledToMagnitude(intercept.strikeProfile.hangTime * arrivalSpeed)

        // Time is chosen with a bias toward hurrying
        val launchPadMoment = intercept.time - intercept.strikeProfile.strikeDuration
        return AnyFacingPreKickWaypoint(
                position = dodgePosition + dodgePositionToHop,
                expectedTime = launchPadMoment,
                waitUntil = if (intercept.spareTime.millis > 0) launchPadMoment else null
        )
    }

    data class SpeedupResult(val forwardMagnitude: Double, val sidewaysMagnitude: Double)

    /**
     * When we are doing either a side dodge or a diagonal dodge, the car will speed up in the forward and horizontal directions.
     */
    private fun getSpeedupResultForAngled(strikeProfile: StrikeProfile, arrivalSpeed: Double): SpeedupResult {
        val sidewaysMultiplier = if (strikeProfile.style == StrikeProfile.Style.DIAGONAL_HIT) .7 else 1.0
        val sidewaysComponent = ManeuverMath.DODGE_SPEED * sidewaysMultiplier

        val forwardMultiplier = if (strikeProfile.style == StrikeProfile.Style.DIAGONAL_HIT) .7 else 0.0
        val forwardComponent = Math.min(arrivalSpeed + ManeuverMath.DODGE_SPEED * forwardMultiplier, AccelerationModel.SUPERSONIC_SPEED)
        return SpeedupResult(forwardComponent, sidewaysComponent)
    }

    private val diagonalSpeedupComponent = 10 * Math.sin(Math.PI / 4)

    private fun diagonalApproach(arrivalSpeed: Double, forceDirection: Vector2, left: Boolean): Vector2 {
        val angle = Math.PI / 4
        return VectorUtil.rotateVector(forceDirection, angle * (if (left) -1.0 else 1.0))
    }

    fun dodgePosition(carPosition: Vector2, carAtContact: Vector2, dodgeDeflectionAngle: Double, dodgeTravel: Double): Vector2? {

        // TODO: add the straight travel that happens during the hop up

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


    fun getAngleOfKickFromApproach(car: CarData, kickPlan: DirectedKickPlan): Double {
        val strikeForceFlat = kickPlan.plannedKickForce.flatten()
        val carPositionAtIntercept = kickPlan.intercept.space
        val carToIntercept = (carPositionAtIntercept - car.position).flatten()
        return carToIntercept.correctionAngle(strikeForceFlat)
    }
}
