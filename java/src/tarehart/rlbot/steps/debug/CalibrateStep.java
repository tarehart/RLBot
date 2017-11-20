package tarehart.rlbot.steps.debug;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static tarehart.rlbot.math.TimeUtil.secondsBetween;
import static tarehart.rlbot.tuning.BotLog.println;

public class CalibrateStep implements Step {

    public static final double TINY_VALUE = .0001;
    private LocalDateTime gameClockStart;
    private LocalDateTime wallClockStart;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (gameClockStart == null && Math.abs(car.spin.yawRate) < TINY_VALUE && car.hasWheelContact) {
            gameClockStart = input.time;
            wallClockStart = LocalDateTime.now();
        }

        if (gameClockStart != null) {
            if (car.spin.yawRate > TINY_VALUE) {
                println(format("Game Latency: %s \nWall Latency: %s",
                        secondsBetween(gameClockStart, input.time),
                        secondsBetween(wallClockStart, now())), input.playerIndex);
                return empty();
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
}
