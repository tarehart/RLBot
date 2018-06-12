package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.bots.Team
import tarehart.rlbot.input.CarData
import tarehart.rlbot.planning.Zone

class TeamIntent(car: CarData, input: AgentInput) {
    var certainty: Int = 0
    var action: TeamAction = TeamAction.WTF
    val car: CarData = car

    init {
        val teamCars = input.getTeamRoster(car.team)
        val carZone = ZonePlan.getZone(car.position)

        //1s logic (kinda need teammates for a team plan)
        if(teamCars.count() == 1) {
            action = TeamAction.WTF
            certainty = 100
        }

        //TODO: 2s logic
        if(teamCars.count() == 2) {
            action = TeamAction.WTF
            certainty = 100
        }

        //TODO: 3s logic
        if(teamCars.count() == 3) {
            //if they are in the opponent's corner, they are usually passing
            if(Zone.isInOpponentCorner(carZone, car.team)) {
                action = TeamAction.PASSING
                certainty = 100
            }
        }

        //4s logic (it's chaos, and it should be chaotic)
        if(teamCars.count() == 4) {
            action = TeamAction.WTF
            certainty = 100
        }
    }
}