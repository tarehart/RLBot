package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BoostPad
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.*
import tarehart.rlbot.routing.CircleTurnUtil
import tarehart.rlbot.routing.SteerPlan
import tarehart.rlbot.time.Duration

import java.awt.*
import java.util.Optional

import tarehart.rlbot.tuning.BotLog.println

class GetBoostStep : NestedPlanStep() {
    private var targetLocation: BoostPad? = null

    public override fun doInitialComputation(input: AgentInput) {

        if (targetLocation == null) {
            init(input)
        } else {
            targetLocation?.let {
                val matchingBoost = input.boostData.fullBoosts.stream()
                        .filter { (location) -> location.distance(it.location) < 1 }.findFirst()

                targetLocation = matchingBoost.orElse(null)
            }
        }

    }

    public override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {

        return input.myCarData.boost > 99 ||
                targetLocation == null || !targetLocation!!.isActive

    }

    override fun doComputationInLieuOfPlan(input: AgentInput): Optional<AgentOutput> {

        val car = input.myCarData
        val targetLoc = targetLocation ?: return Optional.empty()

        val distance = SteerUtil.getDistanceFromCar(car, targetLoc.location)

        if (distance < 3) {
            return Optional.empty()
        } else {

            val myPosition = car.position.flatten()
            val target = targetLoc.location
            val toBoost = target.flatten().minus(myPosition)


            val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost)
            val facing = VectorUtil.orthogonal(target.flatten()) { v -> v.dotProduct(toBoost) > 0 }.normalized()

            val planForCircleTurn = CircleTurnUtil.getPlanForCircleTurn(car, distancePlot, target.flatten(), facing)

            val sensibleFlip = SteerUtil.getSensibleFlip(car, planForCircleTurn.waypoint)
            if (sensibleFlip.isPresent) {
                println("Flipping toward boost", input.playerIndex)
                return startPlan(sensibleFlip.get(), input)
            }

            return Optional.of(planForCircleTurn.immediateSteer)
        }
    }

    private fun init(input: AgentInput) {
        targetLocation = getTacticalBoostLocation(input)
    }

    override fun getLocalSituation(): String {
        return "Going for boost"
    }


    private fun getTacticalBoostLocation(input: AgentInput): BoostPad? {
        var nearestLocation: BoostPad? = null
        var minTime = java.lang.Double.MAX_VALUE
        val carData = input.myCarData
        val distancePlot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4.0), carData.boost)
        for (boost in input.boostData.fullBoosts) {
            val travelSeconds = AccelerationModel.getTravelSeconds(carData, distancePlot, boost.location)
            if (travelSeconds.isPresent && travelSeconds.get().seconds < minTime &&
                    (boost.isActive || travelSeconds.get().minus(Duration.between(input.time, boost.activeTime)).seconds > 1)) {

                minTime = travelSeconds.get().seconds
                nearestLocation = boost
            }
        }
        if (minTime < 1.5) {
            return nearestLocation
        }

        val ballPath = ArenaModel.predictBallPath(input)
        val endpoint = ballPath.endpoint.space
        // Add a defensive bias.
        val idealPlaceToGetBoost = Vector3(endpoint.x, 40 * Math.signum(GoalUtil.getOwnGoal(input.team).center.y), 0.0)
        return getNearestBoost(input.boostData.fullBoosts, idealPlaceToGetBoost)
    }

    private fun getNearestBoost(boosts: List<BoostPad>, position: Vector3): BoostPad? {
        var location: BoostPad? = null
        var minDistance = java.lang.Double.MAX_VALUE
        for (boost in boosts) {
            if (boost.isActive) {
                val distance = position.distance(boost.location)
                if (distance < minDistance) {
                    minDistance = distance
                    location = boost
                }
            }
        }
        return location
    }
}
