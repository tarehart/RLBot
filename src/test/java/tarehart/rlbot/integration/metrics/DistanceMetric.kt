package tarehart.rlbot.integration.metrics

import tarehart.rlbot.time.Duration

class DistanceMetric(distance: Double) : IntegrationMetric<Double, Double>("Distance", distance) {

    override fun quantify(): Double {
        return value
    }

}