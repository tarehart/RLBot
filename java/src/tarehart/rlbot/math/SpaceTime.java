package tarehart.rlbot.math;

import tarehart.rlbot.math.vector.Vector3;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SpaceTime {

    public Vector3 space;
    public LocalDateTime time;

    public SpaceTime(Vector3 space, LocalDateTime time) {
        this.space = space;
        this.time = time;
    }

    @Override
    public String toString() {
        return "space=" + space + ", time=" + time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
