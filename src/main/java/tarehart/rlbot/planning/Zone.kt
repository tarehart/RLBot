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
        TOPCORNER, //covers the -x corners
        BOTTOMCORNER, //covers the +x corners
        TOPSIDELINE, //covers the -x sideline
        BOTTOMSIDELINE, //covers the +x sideline
        ORANGEBOX,
        BLUEBOX
    }

    override fun toString(): String {
        return mainZone.toString() + " " + subZone
    }

    companion object {
        //this is for functions with logic on simple zone and team values
        //functions that are more complex than this should go in the ZoneUtil class
        fun isInOffensiveCorner(zone: Zone, team: Team): Boolean {
            return isInOffensiveThird(zone, team) && (zone.subZone == SubZone.BOTTOMCORNER || zone.subZone == SubZone.TOPCORNER)
        }

        fun isInDefensiveCorner(zone: Zone, team: Team): Boolean {
            return isInDefensiveThird(zone, team) && (zone.subZone == SubZone.BOTTOMCORNER || zone.subZone == SubZone.TOPCORNER)
        }

        fun isOnOffensiveSidelines(zone: Zone, team: Team): Boolean {
            return isInOffensiveThird(zone, team) && (zone.subZone == SubZone.BOTTOMSIDELINE || zone.subZone == SubZone.TOPSIDELINE)
        }

        fun isOnDefensiveSidelines(zone: Zone, team: Team): Boolean {
            return isInDefensiveThird(zone, team) && (zone.subZone == SubZone.BOTTOMSIDELINE || zone.subZone == SubZone.TOPSIDELINE)
        }

        fun isInOffensiveThird(zone: Zone, team: Team): Boolean {
            return (team == Team.BLUE && zone.mainZone == MainZone.ORANGE) || (team == Team.ORANGE && zone.mainZone == MainZone.BLUE)
        }

        fun isInNeutralThird(zone: Zone, team: Team): Boolean {
            return zone.mainZone == MainZone.MID
        }

        fun isInDefensiveThird(zone: Zone, team: Team): Boolean {
            return (team == Team.BLUE && zone.mainZone == MainZone.BLUE) || (team == Team.ORANGE && zone.mainZone == MainZone.ORANGE)
        }

        fun isInCenterLane(zone: Zone, team: Team): Boolean {
            return zone.subZone != SubZone.BOTTOMSIDELINE && zone.subZone != SubZone.TOPSIDELINE
        }
    }
}
