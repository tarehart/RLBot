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
            val ballIsBehindUs = myGoal.center.distanceSquared(ballPosition) < myGoal.center.distanceSquared(bundle.agentInput.myCarData.position)

            if (enemyCar == null) {
                return ThreatReport(
                        enemyMightBoom = false,
                        enemyShotAligned = false,
                        enemyWinsRace = false,
                        enemyHasBreakaway = false,
                        ballIsBehindUs = ballIsBehindUs)
            }

            val shotAlignment = Vector2.alignment(enemyCar.car.position.flatten(), ballPosition.flatten(), myGoal.center.flatten())
            val intercept = enemyCar.intercept

            var impactSpeed = (enemyCar.car.velocity - bundle.agentInput.ballVelocity).magnitude() + 10 // Fudge it
            if (intercept != null) {
                val enemyCarToStrike = intercept.space - enemyCar.car.position
                val strikeVelocity = enemyCarToStrike.scaledToMagnitude(intercept.accelSlice.speed)
                impactSpeed = (strikeVelocity - bundle.agentInput.ballVelocity).magnitude()
            }

            return ThreatReport(
                    enemyMightBoom = impactSpeed > 30 && shotAlignment > 0.3,
                    enemyShotAligned = shotAlignment > 0.3,
                    enemyWinsRace = bundle.tacticalSituation.ballAdvantage.millis < 0,
                    enemyHasBreakaway = ZoneUtil.isEnemyOffensiveBreakaway(bundle.agentInput.myCarData, enemyCar.car, ballPosition),
                    ballIsBehindUs = ballIsBehindUs
            )

        }

    }
}
