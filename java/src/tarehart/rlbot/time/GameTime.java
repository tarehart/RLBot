package tarehart.rlbot.time;

public class GameTime implements Comparable<GameTime> {

    private final long gameMillis;


    /**
     * @param gameSeconds reported by the RLBot framework
     */
    public static GameTime fromGameSeconds(double gameSeconds) {
        return new GameTime((long) (gameSeconds * 1000));
    }

    private GameTime(long gameMillis) {
        this.gameMillis = gameMillis;
    }

    public boolean isBefore(GameTime other) {
        return compareTo(other) < 0;
    }

    public boolean isAfter(GameTime other) {
        return compareTo(other) > 0;
    }

    public static GameTime from(GameTime time) {
        return new GameTime(time.gameMillis);
    }

    public long toMillis() {
        return gameMillis;
    }

    public GameTime plus(Duration duration) {
        return new GameTime(gameMillis + duration.toMillis());
    }

    public GameTime plusSeconds(double seconds) {
        return plus(Duration.ofSeconds(seconds));
    }

    public GameTime minus(Duration duration) {
        return new GameTime(gameMillis - duration.toMillis());
    }

    public GameTime minusSeconds(double seconds) {
        return minus(Duration.ofSeconds(seconds));
    }

    public static GameTime now() {
        return new GameTime(0);
    }


    @Override
    public String toString() {
        return String.format("%.2f", gameMillis / 1000.0);
    }

    @Override
    public int compareTo(GameTime other) {
        return Long.compare(gameMillis, other.gameMillis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GameTime that = (GameTime) o;

        return gameMillis == that.gameMillis;
    }

    @Override
    public int hashCode() {
        return (int) (gameMillis ^ (gameMillis >>> 32));
    }


}
