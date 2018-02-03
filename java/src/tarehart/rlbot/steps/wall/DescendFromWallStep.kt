package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.TacticsTelemetry
import tarehart.rlbot.steps.Step

import java.awt.*
import java.util.Optional

class DescendFromWallStep : Step {

    override val situation = "Descending wall."

    override fun getOutput(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        if (ArenaModel.isCarOnWall(car)) {
            val ballShadow = Vector3(input.ballPosition.x, input.ballPosition.y, 0.0)
            return SteerUtil.steerTowardWallPosition(car, ballShadow)
        } else if (ArenaModel.isNearFloorEdge(car)) {
            return SteerUtil.steerTowardGroundPosition(car, input.ballPosition)
        }

        return null
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
