package tarehart.rlbot.hoops
import tarehart.rlbot.bots.Team
import tarehart.rlbot.math.vector.Vector3
import java.util.*

enum class HoopsZoneTeamless {
    FORWARD_LEFT,
    FORWARD_RIGHT,
    CENTER,
    WIDE_LEFT,
    WIDE_RIGHT;

    operator fun contains(zone: HoopsZone): Boolean {
        return this == HoopsZoneTeamless.FORWARD_LEFT && zone in arrayOf(HoopsZone.KICKOFF_BLUE_FORWARD_LEFT, HoopsZone.KICKOFF_ORANGE_FORWARD_LEFT)
        ||     this == HoopsZoneTeamless.FORWARD_RIGHT && zone in arrayOf(HoopsZone.KICKOFF_BLUE_FORWARD_RIGHT, HoopsZone.KICKOFF_ORANGE_FORWARD_RIGHT)
        ||     this == HoopsZoneTeamless.CENTER && zone in arrayOf(HoopsZone.KICKOFF_BLUE_CENTER, HoopsZone.KICKOFF_ORANGE_CENTER)
        ||     this == HoopsZoneTeamless.WIDE_LEFT && zone in arrayOf(HoopsZone.KICKOFF_BLUE_WIDE_LEFT, HoopsZone.KICKOFF_ORANGE_WIDE_LEFT)
        ||     this == HoopsZoneTeamless.WIDE_RIGHT && zone in arrayOf(HoopsZone.KICKOFF_BLUE_WIDE_RIGHT, HoopsZone.KICKOFF_ORANGE_WIDE_RIGHT)
    }
}

enum class HoopsZone(val center: Vector3, val toleranceSquared: Double = 0.25, val isBlueTeam: Boolean = true) {

    // These are in ordinal order of priority
    // Closest to the ball goes first, followed by the left most.
    KICKOFF_BLUE_FORWARD_LEFT(Vector3(-5.12, -56.3, 0.0), isBlueTeam = true),
    KICKOFF_BLUE_FORWARD_RIGHT(Vector3(5.12, -56.3, 0.0), isBlueTeam = true),
    KICKOFF_BLUE_CENTER(Vector3(0.0, -64.0, 0.0), isBlueTeam = true),
    KICKOFF_BLUE_WIDE_LEFT(Vector3(-30.72, -61.43, 0.0), isBlueTeam = true),
    KICKOFF_BLUE_WIDE_RIGHT(Vector3(30.72, -61.43, 0.0), isBlueTeam = true),
    KICKOFF_ORANGE_FORWARD_LEFT(KICKOFF_BLUE_FORWARD_LEFT.center * -1.0, isBlueTeam = false),
    KICKOFF_ORANGE_FORWARD_RIGHT(KICKOFF_BLUE_FORWARD_RIGHT.center * -1.0, isBlueTeam = false),
    KICKOFF_ORANGE_CENTER(KICKOFF_BLUE_CENTER.center * -1.0, isBlueTeam = false),
    KICKOFF_ORANGE_WIDE_LEFT(KICKOFF_BLUE_WIDE_LEFT.center * -1.0, isBlueTeam = false),
    KICKOFF_ORANGE_WIDE_RIGHT(KICKOFF_BLUE_WIDE_RIGHT.center * -1.0, isBlueTeam = false);
    
    fun isInZone(position: Vector3): Boolean {
        return center.distanceSquared(position) <= toleranceSquared
    }

    fun hasAdvantageAgainst(zone: HoopsZone): Boolean {
        return this.ordinal < zone.ordinal
    }

    companion object {
        fun getZone(position: Vector3): HoopsZone? {
            for (zone in HoopsZone.values()) {
                if (zone.isInZone(position)) {
                    return zone
                }
            }
            return null
        }

        fun getTeamedZone(teamless: HoopsZoneTeamless, team: Team) : HoopsZone {
            val blue = team == Team.BLUE
            when (teamless) {
                HoopsZoneTeamless.CENTER -> return if (blue) KICKOFF_BLUE_CENTER else KICKOFF_ORANGE_CENTER
                HoopsZoneTeamless.FORWARD_LEFT -> return if (blue) KICKOFF_BLUE_FORWARD_LEFT else KICKOFF_ORANGE_FORWARD_LEFT
                HoopsZoneTeamless.FORWARD_RIGHT -> return if (blue) KICKOFF_BLUE_FORWARD_RIGHT else KICKOFF_ORANGE_FORWARD_RIGHT
                HoopsZoneTeamless.WIDE_LEFT -> return if (blue) KICKOFF_BLUE_WIDE_LEFT else KICKOFF_ORANGE_WIDE_LEFT
                HoopsZoneTeamless.WIDE_RIGHT -> return if (blue) KICKOFF_BLUE_WIDE_RIGHT else KICKOFF_ORANGE_WIDE_RIGHT
            }
        }

        fun getZoneById(id: Int): HoopsZone {
            return values()[id]
        }

        fun getRandomZone() : HoopsZone {
            return getZoneById(Random().nextInt(10))
        }
    }
}