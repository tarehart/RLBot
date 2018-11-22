package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.tuning.BotLog

class TeamPlan(input: AgentInput) {
    val teamIntents = input.allCars.map { TeamIntent(it, input) }
}
