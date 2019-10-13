package tarehart.rlbot.steps.defense

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.ZoneUtil

class ThreatAssessor {


    companion object {
        fun getThreatReport(bundle: TacticalBundle): ThreatReport {

            val enemyCar = bundle.tacticalSituation.enemyPlayerWithInitiative

            val myGoal = GoalUtil.getOwnGoal(bundle.agentInput.team)
            val ballPosition = bundle.agentInput.ballPosition
            val car = bundle.agentInput.myCarData
            val ballIsBehindUs = myGoal.center.distanceSquared(ballPosition) < myGoal.center.distanceSquared(car.position)

            if (enemyCar == null) {
                return ThreatReport(
                        enemyMightBoom = false,
                        enemyShotAligned = false,
                        enemyWinsRace = false,
                        enemyHasBreakaway = false,
                        ballIsBehindUs = ballIsBehindUs,
                        enemyDribbling = false,
                        challengeImminent = false)
            }

            val shotAlignment = Vector2.alignment(enemyCar.car.position.flatten(), ballPosition.flatten(), myGoal.center.flatten())
            val intercept = enemyCar.intercept

            var impactSpeed = (enemyCar.car.velocity - bundle.agentInput.ballVelocity).magnitude() + 10 // Fudge it
            if (intercept != null) {
                val enemyCarToStrike = intercept.space - enemyCar.car.position
                val strikeVelocity = enemyCarToStrike.scaledToMagnitude(intercept.accelSlice.speed)
                impactSpeed = (strikeVelocity - bundle.agentInput.ballVelocity).magnitude()
            }

            val defensiveAlignment = Vector2.alignment(ballPosition.flatten(), car.position.flatten(), myGoal.center.flatten())
            val enemyDistanceFromGoal = myGoal.center.distance(enemyCar.car.position)
            val alignmentBreakaway = defensiveAlignment < 0.7 &&
                    enemyDistanceFromGoal < 100 &&
                    myGoal.center.distance(car.position) > enemyDistanceFromGoal * .6 &&
                    shotAlignment > 0.6

            val enemyDribbling = enemyCar.car.position.distance(bundle.agentInput.ballPosition) < 10 &&
                        enemyCar.car.velocity.flatten().distance(bundle.agentInput.ballVelocity.flatten()) < 10

            var challengeImminent = false
            val enemyIntercept = enemyCar.intercept
            val ourIntercept = bundle.tacticalSituation.expectedContact
            if (enemyIntercept != null && ourIntercept != null) {
                challengeImminent = ourIntercept.space.z < 5 &&
                        bundle.tacticalSituation.ballAdvantage.seconds < 0.3 &&
                        ourIntercept.time.isBefore(car.time.plusSeconds(.5)) &&
                        enemyIntercept.time.isBefore(car.time.plusSeconds(.5))
            }


            return ThreatReport(
                    enemyMightBoom = impactSpeed > 30 && shotAlignment > 0.6,
                    enemyShotAligned = shotAlignment > 0.6,
                    enemyWinsRace = bundle.tacticalSituation.ballAdvantage.millis < 0,
                    enemyHasBreakaway = ZoneUtil.isEnemyOffensiveBreakaway(car, enemyCar.car, ballPosition) || alignmentBreakaway,
                    ballIsBehindUs = ballIsBehindUs,
                    enemyDribbling = enemyDribbling,
                    challengeImminent = challengeImminent && shotAlignment > 0.6
            )

        }

    }
}
