package tarehart.rlbot.math;

public class DistanceTimeSpeed {

    public double distance;
    public double time;
    public double speed;

    public DistanceTimeSpeed(double distance, double time, double speed) {
        this.distance = distance;
        this.time = time;
        this.speed = speed;
    }

    /**
     * Seconds since beginning of acceleration sim
     * @return
     */
    public double getTime() {
        return time;
    }
}
