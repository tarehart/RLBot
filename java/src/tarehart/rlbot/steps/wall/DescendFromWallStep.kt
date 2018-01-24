package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.Step

import java.awt.*
import java.util.Optional

class DescendFromWallStep : Step {

    override val situation = "Descending wall."

    override fun getOutput(input: AgentInput): Optional<AgentOutput> {

        val car = input.myCarData
        if (ArenaModel.isCarOnWall(car)) {
            val ballShadow = Vector3(input.ballPosition.x, input.ballPosition.y, 0.0)
            return Optional.of(SteerUtil.steerTowardWallPosition(car, ballShadow))
        } else if (ArenaModel.isNearFloorEdge(car)) {
            return Optional.of(SteerUtil.steerTowardGroundPosition(car, input.ballPosition))
        }

        return Optional.empty()
    }

    override fun canInterrupt(): Boolean {
        return false
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
