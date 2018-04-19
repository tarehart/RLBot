package tarehart.rlbot.input

import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class Chronometer {

    var gameTime = GameTime(0)
        private set
    private var previousGameTime: GameTime? = null


    val timeDiff: Duration
        get() = previousGameTime?.let { Duration.between(it, gameTime) } ?: Duration.ofMillis(100)

    fun readInput(gameSeconds: Double) {
        previousGameTime = gameTime
        gameTime = GameTime.fromGameSeconds(gameSeconds)
    }
}
