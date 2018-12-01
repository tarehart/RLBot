package tarehart.rlbot.integration.metrics

import tarehart.rlbot.time.Duration

class TimeMetric(duration: Duration, name: String = "Time") : IntegrationMetric<Long, Duration>(name, duration) {

    override fun quantify(): Long {
        return value.millis
    }

}