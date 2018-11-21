package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.tuning.BotLog

class TeamPlan(bundle: TacticalBundle) {

    val teamIntents: MutableList<TeamIntent> = mutableListOf<TeamIntent>()
    val ballPosition: Vector3 = input.ballPosition
    val myCar: CarData = input.myCarData


    init {
        // Store data for later use and for telemetry output
        input.allCars.forEach {
            teamIntents.add(TeamIntent(it, input))
        }
    }
}
