package tarehart.rlbot.steps.defense

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.intercept.strike.JumpHitStrike
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

            if (enemyCar?.intercept == null) {
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

            val enemyCarToStrike = intercept.space - enemyCar.car.position
            val strikeVelocity = enemyCarToStrike.scaledToMagnitude(intercept.accelSlice.speed)
            val impactSpeed = (strikeVelocity - bundle.agentInput.ballVelocity).magnitude()

            val enemyDribbling = enemyCar.car.position.distance(bundle.agentInput.ballPosition) < 10 &&
                        enemyCar.car.velocity.flatten().distance(bundle.agentInput.ballVelocity.flatten()) < 10

            var challengeImminent = false
            val enemyIntercept = enemyCar.intercept
            val ourIntercept = bundle.tacticalSituation.expectedContact
            if (ourIntercept != null) {
                challengeImminent = ourIntercept.space.z < 5 &&
                        bundle.tacticalSituation.ballAdvantage.seconds < 0.3 &&
                        ourIntercept.time.isBefore(car.time.plusSeconds(1.0)) &&
                        enemyIntercept.time.isBefore(car.time.plusSeconds(1.0))
            }


            return ThreatReport(
                    enemyMightBoom = impactSpeed > 30 && shotAlignment > 0.6 && enemyIntercept.space.z < JumpHitStrike.MAX_BALL_HEIGHT_FOR_JUMP_HIT,
                    enemyShotAligned = shotAlignment > 0.6,
                    enemyWinsRace = bundle.tacticalSituation.ballAdvantage.millis < 0,
                    enemyHasBreakaway = ZoneUtil.isEnemyOffensiveBreakaway(car, enemyCar.car, ballPosition),
                    ballIsBehindUs = ballIsBehindUs,
                    enemyDribbling = enemyDribbling,
                    challengeImminent = challengeImminent && shotAlignment > 0.6
            )

        }

    }
}
