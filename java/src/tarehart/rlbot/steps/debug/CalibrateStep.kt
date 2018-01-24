package tarehart.rlbot.steps.debug

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.steps.Step
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

import java.awt.*
import java.time.LocalDateTime
import java.util.Optional


class CalibrateStep : Step {
    private lateinit var gameClockStart: GameTime
    private lateinit var wallClockStart: LocalDateTime

    override val situation = "Calibrating"

    override fun getOutput(input: AgentInput): AgentOutput? {

        val car = input.myCarData

        if (!::gameClockStart.isInitialized && Math.abs(car.spin.yawRate) < TINY_VALUE && car.hasWheelContact) {
            gameClockStart = input.time
            wallClockStart = LocalDateTime.now()
        }

        if (::gameClockStart.isInitialized) {
            if (car.spin.yawRate > TINY_VALUE) {
                BotLog.println(String.format("Game Latency: %s \nWall Latency: %s",
                        Duration.between(gameClockStart, input.time).seconds,
                        java.time.Duration.between(wallClockStart, LocalDateTime.now()).toMillis() / 1000.0), input.playerIndex)
                return null
            }
            return AgentOutput().withSteer(1.0).withAcceleration(1.0)
        }

        return AgentOutput().withAcceleration(1.0)
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }

    companion object {
        private const val TINY_VALUE = .0001
    }
}
