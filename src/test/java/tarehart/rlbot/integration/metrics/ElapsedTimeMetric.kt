package tarehart.rlbot.integration.metrics

import tarehart.rlbot.time.Duration

class ElapsedTimeMetric(duration: Duration) : IntegrationMetric<Long, Duration>("Elapsed time", duration) {

    override fun quantify(): Long {
        return value.millis
    }

}