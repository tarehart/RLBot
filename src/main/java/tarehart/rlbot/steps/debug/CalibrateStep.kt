package tarehart.rlbot.steps.debug

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.BotLog

import java.time.LocalDateTime


class CalibrateStep : StandardStep() {
    private lateinit var gameClockStart: GameTime
    private lateinit var wallClockStart: LocalDateTime

    override val situation = "Calibrating"

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val car = bundle.myCarData

        if (!::gameClockStart.isInitialized && Math.abs(car.spin.yawRate) < TINY_VALUE && car.hasWheelContact) {
            gameClockStart = bundle.time
            wallClockStart = LocalDateTime.now()
        }

        if (::gameClockStart.isInitialized) {
            if (car.spin.yawRate > TINY_VALUE) {
                BotLog.println(String.format("Game Latency: %s \nWall Latency: %s",
                        Duration.between(gameClockStart, bundle.time).seconds,
                        java.time.Duration.between(wallClockStart, LocalDateTime.now()).toMillis() / 1000.0), bundle.playerIndex)
                return null
            }
            return AgentOutput().withSteer(1.0).withThrottle(1.0)
        }

        return AgentOutput().withThrottle(1.0)
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    companion object {
        private const val TINY_VALUE = .0001
    }
}
