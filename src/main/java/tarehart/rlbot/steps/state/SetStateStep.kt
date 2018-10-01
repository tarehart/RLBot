package tarehart.rlbot.steps.state

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.GameState
import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.steps.StandardStep

class SetStateStep(private val gameState: GameState) : StandardStep() {
    override val situation = "Setting state"
    private var stateHasBeenSet = false

    override fun getOutput(input: AgentInput): AgentOutput? {
        if (stateHasBeenSet)
            return null

        RLBotDll.setGameState(gameState.buildPacket())
        stateHasBeenSet = true
        return AgentOutput()
    }
}
