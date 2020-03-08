package tarehart.rlbot.integration.metrics

class DistanceMetric(distance: Float, name: String = "Distance") : IntegrationMetric<Float, Float>(name, distance) {

    override fun quantify(): Float {
        return value
    }

}
