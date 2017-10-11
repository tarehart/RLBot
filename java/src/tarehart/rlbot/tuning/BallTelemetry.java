package tarehart.rlbot.tuning;

import tarehart.rlbot.Bot;
import tarehart.rlbot.physics.BallPath;

import java.util.*;

public class BallTelemetry {

    private static Map<Bot.Team, BallPath> ballPaths = new HashMap<>();


    public static void setPath(BallPath ballPath, Bot.Team team) {
        ballPaths.put(team, ballPath);
    }

    public static void reset(Bot.Team team) {
        ballPaths.remove(team);
    }

    public static Optional<BallPath> getPath(Bot.Team team) {
        return Optional.ofNullable(ballPaths.get(team));
    }
}
