package tarehart.rlbot

import tarehart.rlbot.input.CarData
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SetPieces
import tarehart.rlbot.planning.TacticsAdvisor
import tarehart.rlbot.time.GameTime

import java.util.Optional

class JumpingBeanBot(team: Bot.Team, playerIndex: Int) : Bot(team, playerIndex) {

    override fun getOutput(input: AgentInput): AgentOutput {

        if (Plan.activePlanKt(currentPlan) == null) {
            currentPlan = SetPieces.jumpSuperHigh(10.0)
        }

        return currentPlan?.getOutput(input) ?: AgentOutput()
    }
}
