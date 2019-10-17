package tarehart.rlbot.tactics

import tarehart.rlbot.steps.GoForKickoffStep

data class KickoffAdvice(val dodgeRange: Double, val counterAttack: Boolean, val kickoffType: GoForKickoffStep.KickoffType)
