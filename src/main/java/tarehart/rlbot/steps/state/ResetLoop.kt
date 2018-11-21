package tarehart.rlbot.steps.state

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.GameState
import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class ResetLoop(private val gameState: () -> GameState, private val duration: Duration) {

    private var nextReset = GameTime(0)

    /**
     * @param input Current agent input, used for game time
     * @return true if the game has been reset
     */
    fun check(bundle: TacticalBundle) : Boolean {
        if (bundle.agentInput.time > nextReset) {
            reset(bundle)
            return true
        }
        return false
    }

    fun reset(bundle: TacticalBundle) {
        RLBotDll.setGameState(gameState.invoke().buildPacket())
        nextReset = bundle.agentInput.time + duration
    }

}
