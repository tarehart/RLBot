package tarehart.rlbot.integration.metrics

import tarehart.rlbot.time.Duration

class ElapsedTimeMetric(duration: Duration, name: String = "Elapsed time") : IntegrationMetric<Long, Duration>(name, duration) {

    override fun quantify(): Long {
        return value.millis
    }

}