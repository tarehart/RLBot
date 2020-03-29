package tarehart.rlbot.bots

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.debug.CalibrateStep

class LatencyBot(team: Team, playerIndex: Int) : BaseBot(team, playerIndex) {

    private var hasJumped: Boolean = false

    private var calibrateStep = CalibrateStep()

    override fun getOutput(input: AgentInput): AgentOutput {

        if (!hasJumped) {
            if (VectorUtil.flatDistance(input.ballPosition, Vector3()) > 0) {
                hasJumped = true
                return AgentOutput().withJump()
            }

            val car = input.myCarData
            return SteerUtil.steerTowardGroundPosition(car, input.ballPosition.plus(Vector3(20.0, 0.0, 0.0)))
        }

        val output = calibrateStep.getOutput(input)
        if (output != null) {
            return output
        }

        calibrateStep = CalibrateStep()
        return AgentOutput()
    }
}
