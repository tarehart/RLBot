package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector2;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WaypointTelemetry {

    private static Map<Bot.Team, Vector2> waypoints = new HashMap<>();


    public static void set(Vector2 currentWaypoint, Bot.Team team) {
        waypoints.put(team, currentWaypoint);
    }

    public static void reset(Bot.Team team) {
        waypoints.remove(team);
    }

    public static Optional<Vector2> get(Bot.Team team) {
        return Optional.ofNullable(waypoints.get(team));
    }
}
