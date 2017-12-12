package tarehart.rlbot.time;

public class Duration implements Comparable<Duration> {

    private final long millis;

    private Duration(long millis) {
        this.millis = millis;
    }

    public long toMillis() {
        return millis;
    }

    public static Duration between(GameTime first, GameTime later) {
        return new Duration(later.toMillis() - first.toMillis());
    }

    public static Duration ofMillis(long millis) {
        return new Duration(millis);
    }

    public static Duration ofSeconds(double seconds) {
        return new Duration((long) (seconds * 1000));
    }

    public Duration abs() {
        return  millis < 0 ? new Duration(-millis) : this;
    }

    public double getSeconds() {
        return millis / 1000.0;
    }

    @Override
    public int compareTo(Duration other) {
        return Long.compare(millis, other.millis);
    }

    public Duration plusSeconds(double seconds) {
        return new Duration(millis + (long) (seconds * 1000));
    }
}
