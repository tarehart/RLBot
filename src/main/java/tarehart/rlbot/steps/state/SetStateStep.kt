package tarehart.rlbot.steps.state

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.GameState
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.steps.StandardStep

class SetStateStep(private val gameState: GameState) : StandardStep() {
    override val situation = "Setting state"
    private var stateHasBeenSet = false

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {
        if (stateHasBeenSet)
            return null

        RLBotDll.setGameState(gameState.buildPacket())
        stateHasBeenSet = true
        return AgentOutput()
    }
}
