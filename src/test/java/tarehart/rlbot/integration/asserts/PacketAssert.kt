package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.integration.metrics.IntegrationMetric
import tarehart.rlbot.time.Duration

abstract class PacketAssert(val timeLimit: Duration) {

    open var status: AssertStatus = AssertStatus.PENDING
    open var message: String? = null

    val metrics : ArrayList<IntegrationMetric<*, *>> = arrayListOf()

    fun addMetric(metric: IntegrationMetric<*, *>) {
        metrics.add(metric)
    }

    abstract fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?)
}