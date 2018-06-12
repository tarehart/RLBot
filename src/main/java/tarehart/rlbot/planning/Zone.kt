package tarehart.rlbot.planning

import tarehart.rlbot.bots.Team

class Zone(mainZone: MainZone, subZone: SubZone) {
    var mainZone: MainZone = mainZone
    var subZone: SubZone = subZone

    enum class MainZone constructor(private val mainZoneId: Int) {
        NONE(-100),
        ORANGE(-1),
        MID(0),
        BLUE(1)
    }

    enum class SubZone {
        NONE,
        TOP,
        BOTTOM,
        TOPCORNER,
        BOTTOMCORNER,
        ORANGEBOX,
        BLUEBOX
    }

    override fun toString(): String {
        return mainZone.toString() + " " + subZone
    }

    companion object {
        //functions with logic on simple zone and team values
        //functions that are more complex than this should go in the ZoneUtil class
        fun isInOpponentCorner(zone: Zone, team: Team): Boolean {
            return (team === Team.BLUE && zone.mainZone == MainZone.ORANGE || team === Team.ORANGE && zone.mainZone == MainZone.BLUE)
                    && (zone.subZone == SubZone.BOTTOMCORNER || zone.subZone == SubZone.TOPCORNER)
        }
    }
}
