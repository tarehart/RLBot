package tarehart.rlbot.planning

import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.tactics.TacticalSituation

class TeamIntent(val car: CarData, input: AgentInput, situation: TacticalSituation) {
    var certainty: Int = 0
    var action: TeamAction = TeamAction.WTF
    val hasPossession: Boolean = ZoneUtil.carHasPossession(car, situation)

    init {
        val teamCars = input.getTeamRoster(car.team)
        val carZone = ZonePlan.getZone(car.position)
        val ownGoal = GoalUtil.getOwnGoal(car.team)

        //1s logic (kinda need teammates for a team plan)
        if (teamCars.count() == 1) {
            action = TeamAction.WTF
            certainty = 100
        }

        //TODO: 2s logic
        if (teamCars.count() == 2) {
            action = TeamAction.WTF
            certainty = 100
        }

        //TODO: 3s logic
        if (teamCars.count() == 3) {
            //logic for when the car has possession
            if (hasPossession) {
                //if they are in the opponent's corner, they are usually passing
                if (certainty < 90 && Zone.isInOffensiveCorner(carZone, car.team)) {
                    action = TeamAction.PASSING
                    certainty = 90
                }
                //if they are in their own corner, they are usually clearing
                if (certainty < 90 && Zone.isInDefensiveCorner(carZone, car.team)) {
                    action = TeamAction.CLEARING
                    certainty = 90
                }
                //if they are on the opponents third in the middle, they are usually shooting
                if (certainty < 80 && Zone.isInOffensiveThird(carZone, car.team)) {
                    action = TeamAction.SHOOTING
                    certainty = 80
                }
                //if they are on their own sidelines they are probably trying to clear
                if (certainty < 75 && Zone.isOnDefensiveSidelines(carZone, car.team)) {
                    action = TeamAction.CLEARING
                    certainty = 75
                }
                //if they are on the opponent's sidelines they are probably going to pass
                if (certainty < 50 && Zone.isOnOffensiveSidelines(carZone, car.team)) {
                    action = TeamAction.PASSING
                    certainty = 50
                }
            }
            else
            {
                //if they are closest to the goal on their team, they are probably defending
                if(certainty < 90 && teamCars.sortedBy { it.position.distance(GoalUtil.getOwnGoal(input.team).center) }.first() == car) {
                    action = TeamAction.DEFENDING
                    certainty = 90
                }
                //if they aren't the closest to the ball on their team, and they are in the offensive third in the middle
                //they are probably waiting for the pass
                if(certainty < 80 && Zone.isInOffensiveThird(carZone, car.team) && Zone.isInCenterLane(carZone, car.team)) {
                    action = TeamAction.RECEIVING_PASS
                    certainty = 80
                }

                val toOwnGoal = ownGoal.center - car.position
                if (certainty < 80 && !Zone.isInDefensiveThird(carZone, car.team) && toOwnGoal.dotProduct(car.orientation.noseVector) > 0) {
                    action = TeamAction.ROTATING_OUT
                    certainty = 80
                }
            }
        }

        //4s logic (it's chaos, and it should be chaotic)
        if (teamCars.count() == 4) {
            action = TeamAction.WTF
            certainty = 100
        }
    }
}