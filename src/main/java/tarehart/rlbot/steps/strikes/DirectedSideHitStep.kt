package tarehart.rlbot.steps.strikes

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.BallTouch
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.StrikeProfile
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.CircleTurnUtil
import tarehart.rlbot.routing.SteerPlan
import tarehart.rlbot.routing.StrikePoint
import tarehart.rlbot.steps.NestedPlanStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog
import tarehart.rlbot.tuning.ManeuverMath

import java.awt.*
import java.util.Optional

import java.lang.String.format
import tarehart.rlbot.tuning.BotLog.println

class DirectedSideHitStep(private val kickStrategy: KickStrategy) : NestedPlanStep() {


    private lateinit var originalIntercept: Vector3
    private lateinit var originalTouch: Optional<BallTouch>
    private var doneMoment: GameTime? = null
    private lateinit var interceptModifier: Vector3
    private var maneuverSeconds = 0.0
    private var finalApproach = false
    private var recentCircleTurnPlan: SteerPlan? = null
    private var recentCar: CarData? = null
    private var recentKickPlan: DirectedKickPlan? = null

    override fun getLocalSituation(): String {
        return "Directed Side Hit"
    }

    override fun doInitialComputation(input: AgentInput) {
        recentCar = input.myCarData
    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData

        doneMoment?.let {
            if (input.time.isAfter(it)) {
                return null
            }
        }

        if (finalApproach) {
            // Freeze the kick plan
            return performFinalApproach(input, recentKickPlan)
        }

        val kickPlan: DirectedKickPlan?
        val strikeProfile = StrikeProfile(maneuverSeconds, 0.0, 0.0, 0.0, StrikeProfile.Style.SIDE_HIT)
        if (::interceptModifier.isInitialized) {
            kickPlan = DirectedKickUtil.planKick(input, kickStrategy, interceptModifier, { strikeProfile }, input.time)
        } else {
            kickPlan = DirectedKickUtil.planKick(input, kickStrategy, { strikeProfile })
        }

        if (kickPlan == null) {
            BotLog.println("Quitting side hit due to failed kick plan.", car.playerIndex)
            return null
        }

        recentKickPlan = kickPlan

        if (!::interceptModifier.isInitialized) {
            val (x, y, z) = kickPlan.plannedKickForce.scaledToMagnitude(-(DISTANCE_AT_CONTACT + GAP_BEFORE_DODGE))
            interceptModifier = Vector3(x, y, z - 1.4) // Closer to ground
        } else if (kickPlan.intercept.spareTime.seconds > 2) {
            return null // Too much time to be waiting around.
        }

        if (!::originalIntercept.isInitialized) {
            originalIntercept = kickPlan.ballAtIntercept.space
            originalTouch = input.latestBallTouch
        } else {
            if (originalIntercept.distance(kickPlan.ballAtIntercept.space) > 30) {
                println("Failed to make the side kick", input.playerIndex)
                return null // Failed to kick it soon enough, new stuff has happened.
            }

            if (input.latestBallTouch.map{ it.position }.orElse(Vector3()) != originalTouch.map { it.position }.orElse(Vector3())) {
                // There has been a new ball touch.
                println("Ball has been touched, quitting side hit", input.playerIndex)
                return null
            }
        }

        val strikeDirection = kickPlan.plannedKickForce.flatten().normalized()
        val carPositionAtIntercept = kickPlan.intercept.space

        val orthogonalPoint = carPositionAtIntercept.flatten()

        val strikeTime = getStrikeTime(carPositionAtIntercept, GAP_BEFORE_DODGE)
        if (!strikeTime.isPresent) {
            return null
        }
        val expectedSpeed = kickPlan.distancePlot.getMotionAfterDistance(car.position.flatten().distance(orthogonalPoint))?.speed ?: 40.0
        val backoff = expectedSpeed * strikeTime.get().seconds + 1

        if (backoff > car.position.flatten().distance(orthogonalPoint)) {
            BotLog.println("Failed the side hit.", car.playerIndex)
            return null
        }

        val carToIntercept = carPositionAtIntercept.minus(car.position).flatten()
        val facingForSideFlip = VectorUtil.orthogonal(strikeDirection) { v -> v.dotProduct(carToIntercept) > 0 }.normalized()

        if (Vector2.angle(carToIntercept, facingForSideFlip) > Math.PI / 3) {
            // If we're doing more than a quarter turn, this is a waste of time.
            return null
        }

        val steerTarget = orthogonalPoint.minus(facingForSideFlip.scaled(backoff))
        val strikePoint = StrikePoint(steerTarget, facingForSideFlip, kickPlan.intercept.time.minus(strikeTime.get()))

        val toOrthogonal = orthogonalPoint.minus(car.position.flatten())

        val distance = toOrthogonal.magnitude()
        val carNose = car.orientation.noseVector.flatten()
        val angle = Vector2.angle(carNose, facingForSideFlip)
        val positionCorrectionForStrike = carToIntercept.correctionAngle(facingForSideFlip)
        if (distance < backoff + 3 && angle < Math.PI / 12 && Math.abs(positionCorrectionForStrike) < Math.PI / 12 && !ManeuverMath.isSkidding(car)) {
            doneMoment = kickPlan.intercept.time.plusSeconds(.5)
            finalApproach = true
            maneuverSeconds = 0.0
            recentCircleTurnPlan = null
            // Done with the circle turn. Drive toward the orthogonal point and wait for the right moment to launch.
            return performFinalApproach(input, kickPlan)
        }


        maneuverSeconds = angle * MANEUVER_SECONDS_PER_RADIAN

        val circleTurnPlan = CircleTurnUtil.getPlanForCircleTurn(input, kickPlan.distancePlot, strikePoint)
        recentCircleTurnPlan = circleTurnPlan

        return getNavigation(input, circleTurnPlan)
    }

    private fun getStrikeTime(carPositionAtIntercept: Vector3, approachDistance: Double): Optional<Duration> {
        return getJumpTime(carPositionAtIntercept).map { t -> t.plusSeconds(ManeuverMath.secondsForSideFlipTravel(approachDistance)) }
    }

    private fun performFinalApproach(input: AgentInput, kickPlan: DirectedKickPlan?): AgentOutput? {

        // You're probably darn close to flip time.

        val strikeDirection = kickPlan!!.plannedKickForce.flatten().normalized()
        val carPositionAtIntercept = kickPlan.intercept.space

        val orthogonalPoint = carPositionAtIntercept.flatten()

        val car = input.myCarData

        val jumpTime = getJumpTime(carPositionAtIntercept)
        if (!jumpTime.isPresent) {
            return null
        }
        val carAtImpact = kickPlan.ballAtIntercept.space.flatten().plus(strikeDirection.scaled(-DISTANCE_AT_CONTACT))
        val toImpact = carAtImpact.minus(car.position.flatten())
        val projectedApproach = VectorUtil.project(toImpact, car.orientation.rightVector.flatten())
        val realApproachDistance = projectedApproach.magnitude()
        val strikeTime = getStrikeTime(carPositionAtIntercept, realApproachDistance)
        if (!strikeTime.isPresent) {
            return null
        }
        val strikeSeconds = strikeTime.get().seconds
        val backoff = car.velocity.magnitude() * strikeSeconds

        val distance = car.position.flatten().distance(orthogonalPoint)
        val distanceCountdown = distance - backoff
        val timeCountdown = Duration.between(car.time.plus(strikeTime.get()), kickPlan.intercept.time).seconds
        if (distanceCountdown < .1 && timeCountdown < .1) {
            // Time to launch!
            val strikeForceCorrection = DirectedKickUtil.getAngleOfKickFromApproach(car, kickPlan)
            return startPlan(SetPieces.jumpSideFlip(strikeForceCorrection > 0, jumpTime.get(), false), input)
        } else {
            //println(format("Side flip soon. Distance: %.2f, Time: %.2f", distanceCountdown, timeCountdown), input.playerIndex)
            return SteerUtil.getThereOnTime(car, SpaceTime(orthogonalPoint.toVector3(), kickPlan.intercept.time))
        }
    }

    private fun getJumpTime(carPositionAtIntercept: Vector3): Optional<Duration> {
        return ManeuverMath.secondsForMashJumpHeight(carPositionAtIntercept.z).map { Duration.ofSeconds(it) }
    }

    private fun getNavigation(input: AgentInput, circleTurnOption: SteerPlan): AgentOutput? {
        val car = input.myCarData

        if (car.boost == 0.0) {
            SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint)?.let {
                println("Front flip toward side hit", input.playerIndex)
                return startPlan(it, input)
            }
        }

        return circleTurnOption.immediateSteer
    }

    override fun drawDebugInfo(graphics: Graphics2D) {

        super.drawDebugInfo(graphics)

        recentCircleTurnPlan?.let {
            graphics.color = Color(190, 129, 200)
            it.drawDebugInfo(graphics, recentCar!!)
        }

        recentKickPlan?.drawDebugInfo(graphics)
    }

    companion object {
        private const val MANEUVER_SECONDS_PER_RADIAN = .1
        private const val GAP_BEFORE_DODGE = 1.5
        private const val DISTANCE_AT_CONTACT = 2.0
    }
}
