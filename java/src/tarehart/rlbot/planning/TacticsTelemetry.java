package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;

import java.util.*;

public class TacticsTelemetry {

    private static Map<Integer, TacticalSituation> tacticalSituations = new HashMap<>();


    public static void set(TacticalSituation situation, int playerIndex) {
        tacticalSituations.put(playerIndex, situation);
    }

    public static void reset(int playerIndex) {
        tacticalSituations.remove(playerIndex);
    }

    public static Optional<TacticalSituation> get(int playerIndex) {
        return Optional.ofNullable(tacticalSituations.get(playerIndex));
    }
}
