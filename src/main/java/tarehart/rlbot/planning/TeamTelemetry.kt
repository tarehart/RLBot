package tarehart.rlbot.planning

import java.util.*

object TeamTelemetry {

    private val teamPlans = HashMap<Int, TeamPlan>()


    operator fun set(teamPlan: TeamPlan, playerIndex: Int) {
        teamPlans[playerIndex] = teamPlan
    }

    fun reset(playerIndex: Int) {
        teamPlans.remove(playerIndex)
    }

    operator fun get(playerIndex: Int): TeamPlan? {
        return teamPlans[playerIndex]
    }
}
