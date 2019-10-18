package tarehart.rlbot.planning

import java.awt.Color

enum class Posture constructor(private val urgency: Int, val color: Color) {
    NEUTRAL(0, Color.WHITE),
    OFFENSIVE(1, Color.RED),
    DEFENSIVE(5, Color.GREEN),
    CLEAR(8, Color.CYAN),
    ESCAPEGOAL(8, Color.WHITE),
    SAVE(10, Color.CYAN),
    SPIKE_CARRY(40, Color.RED),
    KICKOFF(50, Color.RED),
    LANDING(55, Color.WHITE),
    MENU(75, Color.GRAY),
    OVERRIDE(100, Color.BLUE);

    fun lessUrgentThan(other: Posture): Boolean {
        return urgency < other.urgency
    }

    fun canInterrupt(plan: Plan?): Boolean {
        return plan?.let { it.isComplete() || it.posture.lessUrgentThan(this) && it.canInterrupt() } ?: true
    }
}
