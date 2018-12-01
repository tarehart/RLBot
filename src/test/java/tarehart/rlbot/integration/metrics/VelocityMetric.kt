package tarehart.rlbot.integration.metrics

import tarehart.rlbot.time.Duration

class VelocityMetric(velocity: Double, name: String = "Velocity") : IntegrationMetric<Double, Double>(name, velocity) {

    override fun quantify(): Double {
        return value
    }

}