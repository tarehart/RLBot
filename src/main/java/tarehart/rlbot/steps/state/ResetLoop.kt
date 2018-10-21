package tarehart.rlbot.steps.state

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.GameState
import tarehart.rlbot.AgentInput
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class ResetLoop(private val gameState: () -> GameState, private val duration: Duration) {

    private var nextReset = GameTime(0)

    fun check(input: AgentInput) {
        if (input.time > nextReset) {
            RLBotDll.setGameState(gameState.invoke().buildPacket())
            nextReset = input.time + duration
        }
    }

}
