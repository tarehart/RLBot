package tarehart.rlbot.steps.wall

import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.StandardStep
import tarehart.rlbot.tactics.GameMode
import tarehart.rlbot.tactics.GameModeSniffer
import kotlin.math.abs
import kotlin.math.sign

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

//            val aboveGoal = abs(car.position.y) > ArenaModel.BACK_WALL - 5 && abs(car.position.x) < SoccerGoal.EXTENT
//            if (aboveGoal) {
//                return SteerUtil.steerTowardWallPosition(car, Vector3(SoccerGoal.EXTENT * sign(car.velocity.x * .5 + car.position.x), car.position.y, SoccerGoal.GOAL_HEIGHT))
//            }

            if (car.velocity.z < 30 && car.position.z > 15 && car.velocity.normaliseCopy().dotProduct(Vector3(0, 0, -1)) > .8) {
                return AgentOutput().withJump() // Jump down off the wall now that we're moving downward.
            }

            return SteerUtil.steerTowardWallPosition(car, car.position.withZ(0))
        } else if (ArenaModel.isNearFloorEdge(car.position)) {
            return SteerUtil.steerTowardPositionAcrossSeam(car, ballShadow)
        }

        return null
    }

    override fun canInterrupt(): Boolean {
        return false
    }
}
