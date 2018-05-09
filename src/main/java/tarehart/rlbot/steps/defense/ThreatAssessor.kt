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
import tarehart.rlbot.time.Duration

import java.util.Optional

class ThreatAssessor {


    fun measureThreat(input: AgentInput, enemyCarOption: Optional<CarData>): Double {

        val enemyPosture = measureEnemyPosture(input, enemyCarOption)
        val enemyInitiative = measureEnemyInitiative(input, enemyCarOption)
        val ballThreat = measureBallThreat(input) * .3

        val enemyThreat = (if (enemyPosture > 0 && enemyInitiative > .2) 10 else 0).toDouble()

        return enemyThreat + ballThreat

    }

    private fun measureEnemyInitiative(input: AgentInput, enemyCarOption: Optional<CarData>): Double {

        if (!enemyCarOption.isPresent) {
            return 0.0
        }
        val enemyCar = enemyCarOption.get()

        val simDuration = Duration.ofSeconds(4.0)
        val ballPath = ArenaModel.predictBallPath(input)

        val myCar = input.myCarData

        val myInterceptOption = InterceptCalculator.getInterceptOpportunityAssumingMaxAccel(myCar, ballPath, myCar.boost)
        val enemyIntercept = InterceptCalculator.getInterceptOpportunityAssumingMaxAccel(enemyCar, ballPath, enemyCar.boost)

        if (enemyIntercept == null) {
            return 0.0
        }

        if (myInterceptOption == null) {
            return 3.0
        }

        return Duration.between(myInterceptOption.time, enemyIntercept.time).seconds

    }

    private fun measureEnemyPosture(input: AgentInput, enemyCarOption: Optional<CarData>): Double {

        if (!enemyCarOption.isPresent) {
            return 0.0
        }
        val enemyCar = enemyCarOption.get()

        val myGoal = GoalUtil.getOwnGoal(input.team)
        val ballToGoal = myGoal.center.minus(input.ballPosition)

        val carToBall = input.ballPosition.minus(enemyCar.position)
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
