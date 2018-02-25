package tarehart.rlbot.time

class Duration private constructor(val millis: Long) : Comparable<Duration> {

    val seconds: Double
        get() = millis / 1000.0

    fun abs(): Duration {
        return if (millis < 0) Duration(-millis) else this
    }

    override fun compareTo(other: Duration): Int {
        return java.lang.Long.compare(millis, other.millis)
    }

    fun plusSeconds(seconds: Double): Duration {
        return Duration(millis + (seconds * 1000).toLong())
    }

    operator fun minus(other: Duration): Duration {
        return Duration.ofMillis(millis - other.millis)
    }

    operator fun plus(other: Duration): Duration {
        return Duration.ofMillis(millis + other.millis)
    }

    companion object {

        fun between(first: GameTime, later: GameTime): Duration {
            return Duration(later.toMillis() - first.toMillis())
        }

        fun ofMillis(millis: Long): Duration {
            return Duration(millis)
        }

        fun ofSeconds(seconds: Double): Duration {
            return Duration((seconds * 1000).toLong())
        }
    }
}
