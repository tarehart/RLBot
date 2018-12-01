package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.integration.metrics.IntegrationMetric
import tarehart.rlbot.integration.metrics.TimeMetric
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

abstract class PacketAssert(val timeLimit: Duration, val delayWhenBallFloating: Boolean) {

    open var status: AssertStatus = AssertStatus.PENDING
    open var message: String? = null

    val metrics : ArrayList<IntegrationMetric<*, *>> = arrayListOf()

    init {
        metrics.add(TimeMetric(timeLimit, "Time Limit"))
    }

    fun addMetric(metric: IntegrationMetric<*, *>) {
        metrics.add(metric)
    }

    abstract fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?)

    fun hasExpired(bundle: TacticalBundle, startTime: GameTime) : Boolean {
        val duration = bundle.agentInput.time - startTime
        if (duration < timeLimit) return false
        if (delayWhenBallFloating) {
            val nearestPlane = ArenaModel.getNearestPlane(bundle.agentInput.ballPosition)
            // Floor plane
            // Ball radius is 92.75, hence the ball is very close to touching the ground.
            if (nearestPlane.normal != Vector3.UP ||
                nearestPlane.distance(bundle.agentInput.ballPosition) > 93 / 50.0) {
                return false
            }
        }
        return true
    }
}