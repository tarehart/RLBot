package tarehart.rlbot.steps.state

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.GameState
import tarehart.rlbot.AgentInput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class ResetLoop(private val gameState: () -> GameState, private val duration: Duration) {

    private var nextReset = GameTime(0)
    private var lastResetTime = GameTime(0)

    /**
     * @param input Current agent input, used for game time
     * @return true if the game has been reset
     */
    fun check(time: GameTime) : Boolean {
        if (time > nextReset) {
            reset(time)
            return true
        }
        return false
    }

    fun reset(time: GameTime) {
        RLBotDll.setGameState(gameState.invoke().buildPacket())
        lastResetTime = time
        nextReset = time + duration
    }

    fun getDurationSinceReset(time: GameTime) : Duration {
        return time - lastResetTime
    }

}
