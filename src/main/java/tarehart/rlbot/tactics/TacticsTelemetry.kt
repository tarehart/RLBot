package tarehart.rlbot.tactics

import java.util.HashMap

object TacticsTelemetry {

    private val tacticalSituations = HashMap<Int, TacticalSituation>()

    operator fun set(situation: TacticalSituation, playerIndex: Int) {
        tacticalSituations.put(playerIndex, situation)
    }

    fun reset(playerIndex: Int) {
        tacticalSituations.remove(playerIndex)
    }

    operator fun get(playerIndex: Int): TacticalSituation? {
        return tacticalSituations[playerIndex]
    }
}
