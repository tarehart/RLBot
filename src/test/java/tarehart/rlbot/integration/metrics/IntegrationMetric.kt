package tarehart.rlbot.integration.metrics

abstract class IntegrationMetric<T: Comparable<*>, U>(var name: String, val value: U) : kotlin.Comparable<IntegrationMetric<T, U>> {

    /**
     * Quantifies a value so that it may be displayed on a chart.
     */
    abstract fun quantify(): T

    override fun toString(): String {
        return "${name}: ${valueToString()}"
    }

    open fun valueToString(): String {
        return "${value}"
    }

    override fun compareTo(other: IntegrationMetric<T, U>): Int {
        return compareValues(quantify(), other.quantify())
    }
}