package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.GameModeSniffer

class DescendFromWallStep : StandardStep() {

    override val situation = "Descending wall."

    override fun getOutput(bundle: TacticalBundle): AgentOutput? {

        val input = bundle.agentInput
        val car = input.myCarData
        val ballShadow = Vector3(input.ballPosition.x, input.ballPosition.y, 0.0)
        if (ArenaModel.isCarOnWall(car)) {

            if (GameModeSniffer.getGameMode() == GameMode.HOOPS && car.velocity.z < 2) {
                return AgentOutput().withJump() // Get off the wall asap, might be steering into rim.
            }

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
