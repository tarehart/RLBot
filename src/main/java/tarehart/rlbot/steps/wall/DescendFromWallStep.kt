package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.planning.TacticsTelemetry
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.steps.Step

import java.awt.*
import java.util.Optional

class DescendFromWallStep : StandardStep() {

    override val situation = "Descending wall."

    override fun getOutput(input: AgentInput): AgentOutput? {

        val car = input.myCarData
        val ballShadow = Vector3(input.ballPosition.x, input.ballPosition.y, 0.0)
        if (ArenaModel.isCarOnWall(car)) {
            val ballShadow = Vector3(input.ballPosition.x, input.ballPosition.y, 0.0)
            return SteerUtil.steerTowardWallPosition(car, ballShadow)
        } else if (ArenaModel.isNearFloorEdge(car.position)) {
            return SteerUtil.steerTowardPositionAcrossSeam(car, ballShadow)
        }

        return null
    }

    override fun canInterrupt(): Boolean {
        return false
    }
}
