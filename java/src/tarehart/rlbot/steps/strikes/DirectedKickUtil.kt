package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentInput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.AirTouchPlanner
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.routing.StrikePoint
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath

import java.util.Optional
import java.util.function.BiPredicate
import java.util.function.Function

object DirectedKickUtil {
    private val BALL_VELOCITY_INFLUENCE = .3

    fun planKick(input: AgentInput, kickStrategy: KickStrategy, isSideHit: Boolean): Optional<DirectedKickPlan> {
        val interceptModifier = kickStrategy.getKickDirection(input).normaliseCopy().scaled(-2.0)
        val strikeProfile = StrikeProfile()
        return planKick(input, kickStrategy, isSideHit, interceptModifier, { strikeProfile }, input.time)
    }

    fun planKick(
            input: AgentInput,
            kickStrategy: KickStrategy,
            isSideHit: Boolean,
            interceptModifier: Vector3,
            strikeFn: (Vector3) -> StrikeProfile,
            earliestPossibleIntercept: GameTime): Optional<DirectedKickPlan> {

        val car = input.myCarData

        val verticalPredicate =
                if (isSideHit) { carData: CarData, intercept: SpaceTime -> AirTouchPlanner.isJumpHitAccessible(carData, intercept) }
                else { carData, intercept -> AirTouchPlanner.isVerticallyAccessible(carData, intercept) }

        val overallPredicate = { cd: CarData, st: SpaceTime ->
            st.time >= earliestPossibleIntercept - Duration.ofMillis(50) &&
                    verticalPredicate.invoke(cd, st) &&
                    kickStrategy.looksViable(cd, st.space)
        }

        val ballPath = ArenaModel.predictBallPath(input)
        val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(6.0), car.boost, 0.0)

        val interceptOpportunity = InterceptCalculator.getFilteredInterceptOpportunity(
                car, ballPath, distancePlot, interceptModifier, overallPredicate, strikeFn)


        if (!interceptOpportunity.isPresent) {
            return Optional.empty()
        }

        val intercept = interceptOpportunity.get()
        val ballMotion = ballPath.getMotionAt(intercept.time)
        val ballAtIntercept = ballMotion.get()


        val secondsTillImpactRoughly = Duration.between(input.time, ballAtIntercept.time)
        val approachSpeed = distancePlot.getMotionAfterDuration(secondsTillImpactRoughly).map(DistanceTimeSpeed::speed).orElse(AccelerationModel.SUPERSONIC_SPEED)
        val impactSpeed = if (isSideHit) ManeuverMath.DODGE_SPEED else approachSpeed

        val easyForce: Vector3
        if (isSideHit) {
            val carToIntercept = (intercept.space - car.position).flatten()
            val (x, y) = VectorUtil.orthogonal(carToIntercept) { v -> v.dotProduct(interceptModifier.flatten()) < 0 }
            easyForce = Vector3(x, y, 0.0).scaledToMagnitude(impactSpeed)
        } else {
            easyForce = (ballAtIntercept.space - car.position).scaledToMagnitude(impactSpeed)
        }

        val easyKick = bump(ballAtIntercept.velocity, easyForce)
        val kickDirection = kickStrategy.getKickDirection(input, ballAtIntercept.space, easyKick)

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

        val launchPad: StrikePoint?

        if (!isSideHit) {
            val backoff = 5 + (ballAtIntercept.space.z - ArenaModel.BALL_RADIUS) * 2 + intercept.spareTime.seconds * 5
            val facing = plannedKickForce.flatten().normalized()
            val launchPosition = ballAtIntercept.space.flatten() - facing.scaledToMagnitude(backoff)
            // Time is chosen with a bias toward hurrying
            launchPad = StrikePoint(launchPosition, facing, intercept.time - Duration.ofSeconds(backoff / car.velocity.magnitude()))
        } else {
            launchPad = null
        }

        val kickPlan = DirectedKickPlan(
                interceptModifier = interceptModifier,
                ballPath = ballPath,
                distancePlot = distancePlot,
                intercept = interceptOpportunity.get(),
                ballAtIntercept = ballAtIntercept,
                plannedKickForce = plannedKickForce,
                desiredBallVelocity = desiredBallVelocity,
                launchPad = launchPad,
                easyKickAllowed = easyKickAllowed
        )

        return Optional.of(kickPlan)
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
