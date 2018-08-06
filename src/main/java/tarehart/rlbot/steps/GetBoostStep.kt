package tarehart.rlbot.steps

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BoostPad
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.routing.CircleTurnUtil
import tarehart.rlbot.routing.waypoint.StrictPreKickWaypoint
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println

class GetBoostStep : NestedPlanStep() {
    private var targetLocation: BoostPad? = null

    public override fun doInitialComputation(input: AgentInput) {

        if (targetLocation == null) {
            init(input)
        } else {
            targetLocation?.let {
                val matchingBoost = BoostAdvisor.boostData.fullBoosts.stream()
                        .filter { (location) -> location.distance(it.location) < 1 }.findFirst()

                targetLocation = matchingBoost.orElse(null)
            }
        }

    }

    public override fun shouldCancelPlanAndAbort(input: AgentInput): Boolean {

        return input.myCarData.boost > 99 ||
                targetLocation == null || !targetLocation!!.isActive

    }

    override fun doComputationInLieuOfPlan(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        val targetLoc = targetLocation ?: return null

        val distance = SteerUtil.getDistanceFromCar(car, targetLoc.location)

        if (distance < 3) {
            return null
        } else {

            val myPosition = car.position.flatten()
            val target = targetLoc.location
            val toBoost = target.flatten().minus(myPosition)


            val distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4.0), car.boost)
            val facing = VectorUtil.orthogonal(target.flatten()) { v -> v.dotProduct(toBoost) > 0 }.normalized()

            val planForCircleTurn = CircleTurnUtil.getPlanForCircleTurn(input.myCarData, distancePlot, StrictPreKickWaypoint(target.flatten(), facing, input.time))

            SteerUtil.getSensibleFlip(car, planForCircleTurn.waypoint)?.let {
                println("Flipping toward boost", input.playerIndex)
                return startPlan(it, input)
            }

            return planForCircleTurn.immediateSteer
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
        for (boost in BoostAdvisor.boostData.fullBoosts) {
            val travelSeconds = AccelerationModel.getTravelSeconds(carData, distancePlot, boost.location)
            if (travelSeconds != null && travelSeconds.seconds < minTime &&
                    (boost.isActive || travelSeconds.minus(Duration.between(input.time, boost.activeTime)).seconds > 1)) {

                minTime = travelSeconds.seconds
                nearestLocation = boost
            }
        }
        if (minTime < 1.5) {
            return nearestLocation
        }

        val ballPath = ArenaModel.predictBallPath(input)
        val endpoint = ballPath.getMotionAt(input.time.plusSeconds(3.0)) ?: ballPath.endpoint
        // Add a defensive bias.
        val defensiveBias = 50 * Math.signum(GoalUtil.getOwnGoal(input.team).center.y)
        val idealPlaceToGetBoost = Vector3(endpoint.space.x, endpoint.space.y + defensiveBias, 0.0)
        return getNearestBoost(BoostAdvisor.boostData.fullBoosts, idealPlaceToGetBoost)
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
