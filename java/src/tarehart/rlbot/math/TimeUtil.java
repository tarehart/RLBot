package tarehart.rlbot.math;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeUtil {

    public static double secondsBetween(LocalDateTime a, LocalDateTime b) {
        return toSeconds(Duration.between(a, b));
    }

    public static Duration toDuration(double seconds) {
        return Duration.ofNanos(Math.round(seconds * 1E9));
    }

    public static double toSeconds(Duration duration) {
        return duration.toMillis() / 1000.0;
    }
}
