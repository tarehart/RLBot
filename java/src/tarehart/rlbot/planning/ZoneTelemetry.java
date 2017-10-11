package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;

import java.util.*;

public class ZoneTelemetry {

    private static Map<Bot.Team, ZonePlan> zonePlans = new HashMap<>();


    public static void set(ZonePlan zonePlan, Bot.Team team) {
        zonePlans.put(team, zonePlan);
    }

    public static void reset(Bot.Team team) {
        zonePlans.remove(team);
    }

    public static Optional<ZonePlan> get(Bot.Team team) {
        return Optional.ofNullable(zonePlans.get(team));
    }
}
