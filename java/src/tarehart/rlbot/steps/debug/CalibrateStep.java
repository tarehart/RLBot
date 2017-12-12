package tarehart.rlbot.steps.debug;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;
import tarehart.rlbot.tuning.BotLog;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.Optional;


public class CalibrateStep implements Step {
    public static final double TINY_VALUE = .0001;
    private GameTime gameClockStart;
    private LocalDateTime wallClockStart;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (gameClockStart == null && Math.abs(car.spin.yawRate) < TINY_VALUE && car.hasWheelContact) {
            gameClockStart = input.time;
            wallClockStart = LocalDateTime.now();
        }

        if (gameClockStart != null) {
            if (car.spin.yawRate > TINY_VALUE) {
                BotLog.println(String.format("Game Latency: %s \nWall Latency: %s",
                        Duration.between(gameClockStart, input.time).getSeconds(),
                        java.time.Duration.between(wallClockStart, LocalDateTime.now()).toMillis() / 1000.0), input.playerIndex);
                return Optional.empty();
            }
            return Optional.of(new AgentOutput().withSteer(1).withAcceleration(1));
        }

        return Optional.of(new AgentOutput().withAcceleration(1));
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Calibrating";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        // Draw nothing.
    }
}
