package tarehart.rlbot.steps

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.input.BoostPad
import tarehart.rlbot.math.BotMath
import tarehart.rlbot.math.Circle
import tarehart.rlbot.math.Clamper
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.BotLog.println
import java.awt.Color

class GetBoostStep : NestedPlanStep() {
    private var targetLocation: BoostPad? = null

    public override fun doInitialComputation(bundle: TacticalBundle) {

        if (targetLocation == null) {
            init(bundle)
        } else {
            targetLocation?.let {
                val matchingBoost = BoostAdvisor.boostData.fullBoosts.stream()
                        .filter { (location) -> location.distance(it.location) < 1 }.findFirst()

                targetLocation = matchingBoost.orElse(null)
            }
        }

    }

    public override fun shouldCancelPlanAndAbort(bundle: TacticalBundle): Boolean {

        return bundle.agentInput.myCarData.boost > 99 ||
                targetLocation == null || !targetLocation!!.isActive

    }

    override fun doComputationInLieuOfPlan(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.agentInput.myCarData
        val targetLoc = targetLocation ?: return null

        RenderUtil.drawCircle(car.renderer, Circle(targetLoc.location.flatten(), 2), 2, Color.GREEN)
        car.renderer.drawLine3d(Color.GREEN, car.position.toRlbot(), targetLoc.location.withZ(2).toRlbot())

        val distance = SteerUtil.getDistanceFromCar(car, targetLoc.location)

        if (distance < 3) {
            return null
        } else {

            val myPosition = car.position.flatten()
            val target = targetLoc.location.flatten()
            val toBoost = target.minus(myPosition)

            val facing = VectorUtil.orthogonal(target) { v -> v.dotProduct(toBoost) > 0 }.normalized()
            val waypoint = target - facing.scaledToMagnitude(Clamper.clamp(toBoost.magnitude() * 0.3 - 5, 0.0, 20.0))

            SteerUtil.getSensibleFlip(car, waypoint)?.let {
                println("Flipping toward boost", bundle.agentInput.playerIndex)
                return startPlan(it, bundle)
            }

            return SteerUtil.steerTowardGroundPosition(car, waypoint)
        }
    }

    private fun init(bundle: TacticalBundle) {
        targetLocation = getTacticalBoostLocation(bundle)
    }

    override fun getLocalSituation(): String {
        return "Going for boost"
    }

    private fun getTacticalBoostLocation(bundle: TacticalBundle): BoostPad? {
        var nearestLocation: BoostPad? = null
        var minTime = java.lang.Double.MAX_VALUE
        val carData = bundle.agentInput.myCarData
        val distancePlot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4.0), carData.boost)
        for (boost in BoostAdvisor.boostData.fullBoosts) {
            val travelSeconds = AccelerationModel.getTravelSeconds(carData, distancePlot, boost.location)
            if (travelSeconds != null && travelSeconds.seconds < minTime &&
                    (boost.isActive || travelSeconds.minus(Duration.between(bundle.agentInput.time, boost.activeTime)).seconds > 1)) {

                minTime = travelSeconds.seconds
                nearestLocation = boost
            }
        }
        if (minTime < 1.5 || bundle.agentInput.ballVelocity.flatten().isZero) {
            return nearestLocation
        }

        val ballPath = bundle.tacticalSituation.ballPath
        val endpoint = ballPath.getMotionAt(bundle.agentInput.time.plusSeconds(3.0)) ?: ballPath.endpoint
        // Add a defensive bias.
        val defensiveBias = 80 * Math.signum(GoalUtil.getOwnGoal(bundle.agentInput.team).center.y)
        val defensiveLocation = Vector3(endpoint.space.x, endpoint.space.y + defensiveBias, 0.0)
        var idealPlaceToGetBoost = defensiveLocation.scaled(.5) + carData.position.scaled(.5)

        bundle.agentInput.allCars.forEach {
            if (it != carData) {
                val otherToMe = carData.position - it.position
                idealPlaceToGetBoost += otherToMe.scaledToMagnitude(20.0)
            }
        }

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
