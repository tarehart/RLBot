package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.planning.SteerUtil

class MachinimaBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    override fun getOutput(input: AgentInput): AgentOutput {

        val enemyCar = input.getTeamRoster(input.team.opposite())[0]
        val waypoint = enemyCar.position + enemyCar.orientation.rightVector.scaled(3F) + enemyCar.orientation.noseVector.scaled(1F)

        if (input.myCarData.position.distance(waypoint) < 5) {
            return AgentOutput()
        }

        return SteerUtil.getThereOnTime(input.myCarData, SpaceTime(waypoint, input.time.plusSeconds(0.5)))
    }
}
