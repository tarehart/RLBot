package tarehart.rlbot.integration.metrics

class VelocityMetric(velocity: Float, name: String = "Velocity") : IntegrationMetric<Float, Float>(name, velocity) {

    override fun quantify(): Float {
        return value
    }

}
