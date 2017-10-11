package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;

import java.util.*;

public class TacticsTelemetry {

    private static Map<Bot.Team, TacticalSituation> tacticalSituations = new HashMap<>();


    public static void set(TacticalSituation situation, Bot.Team team) {
        tacticalSituations.put(team, situation);
    }

    public static void reset(Bot.Team team) {
        tacticalSituations.remove(team);
    }

    public static Optional<TacticalSituation> get(Bot.Team team) {
        return Optional.ofNullable(tacticalSituations.get(team));
    }
}
