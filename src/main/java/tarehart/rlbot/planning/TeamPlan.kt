package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.tuning.BotLog

class TeamPlan(input: AgentInput) {

    val teamIntents: MutableList<TeamIntent> = mutableListOf<TeamIntent>()
    val ballPosition: Vector3 = input.ballPosition
    val myCar: CarData = input.myCarData


    init {
        // Store data for later use and for telemetry output
        input.getTeamRoster(input.team).forEach {
            //make sure we skip ourselves
            if(it.playerIndex != myCar.playerIndex){
                teamIntents.add(TeamIntent(it, input))
            }
        }
    }
}
