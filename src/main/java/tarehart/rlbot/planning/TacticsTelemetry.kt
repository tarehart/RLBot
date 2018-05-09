package tarehart.rlbot.planning

import java.util.HashMap
import java.util.Optional

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
