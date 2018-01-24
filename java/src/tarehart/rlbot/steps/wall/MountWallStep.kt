package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentInput
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.Step

import java.awt.*
import java.util.Optional

class MountWallStep : Step {

    override val situation = "Mounting the wall"

    override fun getOutput(input: AgentInput): Optional<AgentOutput> {

        val car = input.myCarData

        if (ArenaModel.isCarOnWall(car)) {
            // Successfully made it onto the wall
            return Optional.empty()
        }

        val ballPath = ArenaModel.predictBallPath(input)
        if (!WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
            // Failed to mount the wall in time.
            return Optional.empty()
        }

        val (space) = ballPath.getMotionAt(input.time.plusSeconds(3.0)).orElse(ballPath.endpoint)
        val ballPositionExaggerated = space.scaled(1.04) // This assumes the ball is close to the wall

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, ballPositionExaggerated))
    }

    override fun canInterrupt(): Boolean {
        return true
    }

    override fun drawDebugInfo(graphics: Graphics2D) {
        // Draw nothing.
    }
}
