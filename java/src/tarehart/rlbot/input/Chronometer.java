package tarehart.rlbot.input;

import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

public class Chronometer {

    private GameTime gameTime;
    private GameTime previousGameTime;

    public Chronometer() {
        gameTime = GameTime.now();
        previousGameTime = null;
    }

    public void readInput(double gameSeconds) {
        previousGameTime = gameTime;
        gameTime = GameTime.fromGameSeconds(gameSeconds);
    }

    public GameTime getGameTime() {
        return gameTime;
    }

    public Duration getTimeDiff() {
        if (previousGameTime != null) {
            return Duration.between(previousGameTime, gameTime);
        }
        return Duration.ofMillis(100); // This should be extremely rare.
    }
}
