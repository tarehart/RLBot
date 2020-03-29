package tarehart.rlbot.steps.debug

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

import java.time.LocalDateTime


class CalibrateStep {
    private lateinit var gameClockStart: GameTime
    private lateinit var wallClockStart: LocalDateTime

    val situation = "Calibrating"

    fun getOutput(agentInput: AgentInput): AgentOutput? {

        val car = agentInput.myCarData

        if (!::gameClockStart.isInitialized && Math.abs(car.spin.yawRate) < TINY_VALUE && car.hasWheelContact) {
            gameClockStart = agentInput.time
            wallClockStart = LocalDateTime.now()
        }

        if (::gameClockStart.isInitialized) {
            if (car.spin.yawRate > TINY_VALUE) {
                BotLog.println(String.format("Game Latency: %s \nWall Latency: %s",
                        Duration.between(gameClockStart, agentInput.time).seconds,
                        java.time.Duration.between(wallClockStart, LocalDateTime.now()).toMillis() / 1000.0), agentInput.playerIndex)
                return null
            }
            return AgentOutput().withSteer(1.0).withThrottle(1.0)
        }

        return AgentOutput().withThrottle(1.0)
    }

    companion object {
        private const val TINY_VALUE = .0001
    }
}
