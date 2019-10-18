package tarehart.rlbot.time

data class GameTime(private val gameMillis: Long) : Comparable<GameTime> {

    fun isBefore(other: GameTime): Boolean {
        return this < other
    }

    fun isAfter(other: GameTime): Boolean {
        return this > other
    }

    fun toMillis(): Long {
        return gameMillis
    }

    fun toSeconds(): Float {
        return gameMillis / 1000.0f
    }

    operator fun plus(duration: Duration): GameTime {
        return GameTime(gameMillis + duration.millis)
    }

    fun plusSeconds(seconds: Number): GameTime {
        return plus(Duration.ofSeconds(seconds))
    }

    operator fun minus(duration: Duration): GameTime {
        return GameTime(gameMillis - duration.millis)
    }

    operator fun minus(gameTime: GameTime): Duration {
        return Duration.ofMillis(gameMillis - gameTime.gameMillis)
    }

    fun minusSeconds(seconds: Double): GameTime {
        return minus(Duration.ofSeconds(seconds))
    }


    override fun toString(): String {
        return String.format("%.2f", gameMillis / 1000.0)
    }

    override fun compareTo(other: GameTime): Int {
        return java.lang.Long.compare(gameMillis, other.gameMillis)
    }

    companion object {

        /**
         * @param gameSeconds reported by the RLBot framework
         */
        fun fromGameSeconds(gameSeconds: Double): GameTime {
            return GameTime((gameSeconds * 1000).toLong())
        }

        fun zero(): GameTime {
            return GameTime(0)
        }
    }


}
