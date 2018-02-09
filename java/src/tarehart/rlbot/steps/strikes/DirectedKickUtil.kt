package tarehart.rlbot.steps.strikes

import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.routing.StrikePoint
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath

object DirectedKickUtil {
    private val BALL_VELOCITY_INFLUENCE = .2

    fun planKick(car: CarData, ballPath: BallPath, kickStrategy: KickStrategy, strikeFn: (Double) -> StrikeProfile): DirectedKickPlan? {
        val kickDirection = kickStrategy.getKickDirection(car, ballPath.startPoint.space) ?: return null
        val interceptModifier = kickDirection.normaliseCopy().scaled(-2.0)
        return planKick(car, ballPath, kickStrategy, interceptModifier, strikeFn, car.time)
    }

    fun planKick(
            car: CarData,
            ballPath: BallPath,
            kickStrategy: KickStrategy,
            interceptModifier: Vector3,
            strikeFn: (Double) -> StrikeProfile,
            earliestIntercept: GameTime): DirectedKickPlan? {

        val overallPredicate = { cd: CarData, st: SpaceTime ->
            val strikeProf = strikeFn.invoke(st.space.z)
            val verticallyAccessible = strikeProf.verticallyAccessible.invoke(cd, st)
            val viableKick = kickStrategy.looksViable(cd, st.space)
            verticallyAccessible && viableKick
        }

        // Modify the ball path to drop the slices before the earliest allowed intercept.
        // This is better than passing in a time-based predicate, because that results in "spare time" which is misleading.
        val truncatedBallPath = ballPath.startingFrom(earliestIntercept) ?: return null
        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(6.0), car.boost, 0.0)

        val intercept = InterceptCalculator.getFilteredInterceptOpportunity(
                car, truncatedBallPath, distancePlot, interceptModifier, overallPredicate, strikeFn) ?: return null

        return planKickFromIntercept(intercept, ballPath, car, kickStrategy)
    }

    fun planKickFromIntercept(intercept: Intercept, ballPath: BallPath, car: CarData, kickStrategy: KickStrategy): DirectedKickPlan? {

        //, distancePlot: DistancePlot, interceptModifier: Vector3, kickStrategy: KickStrategy, ballPath: BallPath

        val ballAtIntercept = intercept.ballSlice
        val arrivalSpeed = intercept.accelSlice.speed
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

        val backoff = 5 + (ballAtIntercept.space.z - ArenaModel.BALL_RADIUS) * .05 * ManeuverMath.forwardSpeed(car) + intercept.spareTime.seconds * 5
        val toIntercept = (intercept.space.flatten() - car.position.flatten()).normalized()
        val flatForce = plannedKickForce.flatten()
        val toKickForce = toIntercept.correctionAngle(flatForce)
        val ballPosition = ballAtIntercept.space.flatten()

        val launchPosition: Vector2
        val facing: Vector2

        when (intercept.strikeProfile.style) {
            StrikeProfile.Style.RAM -> {
                facing = VectorUtil.rotateVector(toIntercept, toKickForce * .5)
                launchPosition = intercept.space.flatten()
            }
            StrikeProfile.Style.DIAGONAL_HIT -> {
                facing = diagonalApproach(arrivalSpeed, flatForce, toKickForce > 0)
                launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(intercept.strikeProfile.strikeDuration.seconds * arrivalSpeed)
            }
            StrikeProfile.Style.SIDE_HIT -> {
                facing = VectorUtil.rotateVector(flatForce, -Math.signum(toKickForce) * Math.PI / 2)
                launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(2.0) -
                        facing.scaledToMagnitude(intercept.strikeProfile.strikeDuration.seconds * arrivalSpeed)
            }
            StrikeProfile.Style.FLIP_HIT -> {
                facing = flatForce.normalized()
                launchPosition = intercept.space.flatten() - flatForce.scaledToMagnitude(intercept.strikeProfile.strikeDuration.seconds * arrivalSpeed)
            }
            else -> {
                facing = flatForce.normalized()
                launchPosition = ballPosition - flatForce.scaledToMagnitude(intercept.strikeProfile.strikeDuration.seconds * arrivalSpeed)
            }
        }

        val strikeDuration = if (intercept.strikeProfile.style == StrikeProfile.Style.AERIAL)
            Duration.ofSeconds(backoff / car.velocity.magnitude()) // TODO: duration should be built in
        else
            intercept.strikeProfile.strikeDuration

        // Time is chosen with a bias toward hurrying
        val launchPad = StrikePoint(launchPosition, facing, intercept.time - strikeDuration)


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

    private val diagonalSpeedupComponent = 10 * Math.sin(Math.PI / 4)

    private fun diagonalApproach(arrivalSpeed: Double, forceDirection: Vector2, left: Boolean): Vector2 {
        val angle = Math.asin(diagonalSpeedupComponent / arrivalSpeed)
        return VectorUtil.rotateVector(forceDirection, angle * (if (left) -1.0 else 1.0))
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
