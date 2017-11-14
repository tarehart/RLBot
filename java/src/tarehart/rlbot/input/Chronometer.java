package tarehart.rlbot.input;

import rlbot.api.GameData;
import tarehart.rlbot.math.TimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;

public class Chronometer {

    private LocalDateTime gameTime;
    private LocalDateTime previousGameTime;

    private Double previousGameTimeRemaining = null;
    private Double previousTimeSeconds = null;

    public Chronometer() {
        gameTime = LocalDateTime.now();
        previousGameTime = null;
    }

    public void readInput(GameData.GameInfo timeInfo, boolean isKickoff) {
        readInput(timeInfo.getGameTimeRemaining(), timeInfo.getSecondsElapsed(), isKickoff);
    }

    private void readInput(double gameTimeRemaining, double secondsElapsed, boolean isKickoff) {

        if (previousGameTimeRemaining != null && previousTimeSeconds != null) {
            double deltaSeconds;
            if (gameTimeRemaining > 0 && !isKickoff) {
                deltaSeconds = Math.abs(previousGameTimeRemaining - gameTimeRemaining);
            } else {
                deltaSeconds = secondsElapsed - previousTimeSeconds;
            }

            previousGameTime = gameTime;
            gameTime = gameTime.plus(TimeUtil.toDuration(deltaSeconds));
        }

        previousGameTimeRemaining = gameTimeRemaining;
        previousTimeSeconds = secondsElapsed;
    }

    public LocalDateTime getGameTime() {
        return gameTime;
    }

    public Duration getTimeDiff() {
        if (previousGameTime != null) {
            return Duration.between(previousGameTime, gameTime);
        }
        return Duration.ofMillis(100); // This should be extremely rare.
    }
}
