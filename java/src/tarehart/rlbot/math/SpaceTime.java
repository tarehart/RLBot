package tarehart.rlbot.math;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.time.GameTime;

public class SpaceTime {

    public Vector3 space;
    public GameTime time;

    public SpaceTime(Vector3 space, GameTime time) {
        this.space = space;
        this.time = time;
    }

    @Override
    public String toString() {
        return "space=" + space + ", time=" + time;
    }
}
