package tarehart.rlbot.steps.defense

import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.InterceptCalculator
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.Goal
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.planning.CarWithIntercept
import tarehart.rlbot.time.Duration

import java.util.Optional

class ThreatAssessor {


    fun measureThreat(input: AgentInput, enemyCarOption: CarWithIntercept?): Double {

        val enemyPosture = measureEnemyPosture(input, enemyCarOption)
        var ballThreat = measureBallThreat(input) * .3
        if (ballThreat < 0) {
            ballThreat *= .2
        }

        val enemyThreat = Math.max(0.0, enemyPosture)

        return enemyThreat + ballThreat
    }

    private fun measureEnemyPosture(input: AgentInput, enemyCar: CarWithIntercept?): Double {

        if (enemyCar == null) {
            return 0.0
        }

        val myGoal = GoalUtil.getOwnGoal(input.team)
        val ballToGoal = myGoal.center.minus(input.ballPosition)

        val carToBall = input.ballPosition.minus(enemyCar.car.position)
        val rightSideVector = VectorUtil.project(carToBall, ballToGoal)

        return rightSideVector.magnitude() * Math.signum(rightSideVector.dotProduct(ballToGoal))
    }


    private fun measureBallThreat(input: AgentInput): Double {

        val car = input.myCarData
        val myGoal = GoalUtil.getOwnGoal(input.team)
        val ballToGoal = myGoal.center.minus(input.ballPosition)

        val ballVelocityTowardGoal = VectorUtil.project(input.ballVelocity, ballToGoal)
        val ballSpeedTowardGoal = ballVelocityTowardGoal.magnitude() * Math.signum(ballVelocityTowardGoal.dotProduct(ballToGoal))

        val carToBall = input.ballPosition.minus(car.position)
        val wrongSideVector = VectorUtil.project(carToBall, ballToGoal)
        val wrongSidedness = wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal))

        return ballSpeedTowardGoal + wrongSidedness
    }

}
