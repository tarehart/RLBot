package tarehart.rlbot.steps.defense

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.planning.CarWithIntercept
import tarehart.rlbot.planning.GoalUtil

class ThreatAssessor {


    fun measureThreat(bundle: TacticalBundle, enemyCarOption: CarWithIntercept?): Double {

        val enemyPosture = measureEnemyPosture(bundle, enemyCarOption)
        var ballThreat = measureBallThreat(bundle) * .3
        if (ballThreat < 0) {
            ballThreat *= .1
        }

        val enemyThreat = Math.max(0.0, enemyPosture)

        return enemyThreat + ballThreat
    }

    private fun measureEnemyPosture(bundle: TacticalBundle, enemyCar: CarWithIntercept?): Double {

        if (enemyCar == null) {
            return 0.0
        }

        val myGoal = GoalUtil.getOwnGoal(bundle.agentInput.team)
        val ballToGoal = myGoal.center.minus(bundle.agentInput.ballPosition)

        val carToBall = bundle.agentInput.ballPosition.minus(enemyCar.car.position)
        val rightSideVector = VectorUtil.project(carToBall.scaledToMagnitude(10.0), ballToGoal)

        return rightSideVector.magnitude() * Math.signum(rightSideVector.dotProduct(ballToGoal))
    }


    private fun measureBallThreat(bundle: TacticalBundle): Double {

        val car = bundle.agentInput.myCarData
        val myGoal = GoalUtil.getOwnGoal(bundle.agentInput.team)
        val ballToGoal = myGoal.center.minus(bundle.agentInput.ballPosition)

        val ballVelocityTowardGoal = VectorUtil.project(bundle.agentInput.ballVelocity, ballToGoal)
        val ballSpeedTowardGoal = ballVelocityTowardGoal.magnitude() * Math.signum(ballVelocityTowardGoal.dotProduct(ballToGoal))

        val carToBall = bundle.agentInput.ballPosition.minus(car.position)
        val wrongSideVector = VectorUtil.project(carToBall, ballToGoal)
        val wrongSidedness = wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal))

        return ballSpeedTowardGoal + wrongSidedness - ballToGoal.magnitude() * .3
    }

}
