package tarehart.rlbot.planning

import java.util.*

object ZoneTelemetry {

    private val zonePlans = HashMap<Int, ZonePlan>()


    operator fun set(zonePlan: ZonePlan, playerIndex: Int) {
        zonePlans[playerIndex] = zonePlan
    }

    fun reset(playerIndex: Int) {
        zonePlans.remove(playerIndex)
    }

    operator fun get(playerIndex: Int): ZonePlan? {
        return zonePlans[playerIndex]
    }
}
