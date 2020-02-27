package tarehart.rlbot.steps.defense

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.intercept.strike.JumpHitStrike
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.planning.ZoneUtil
import tarehart.rlbot.time.Duration

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

            val enemyIntercept = enemyCar.intercept
            val enemyFlatPos = enemyCar.car.position.flatten()
            val enemyToIntercept = enemyIntercept.space.flatten() - enemyFlatPos
            val shotAlignment = Vector2.alignment(enemyFlatPos, enemyIntercept.space.flatten(), myGoal.center.flatten())

            val enemyCarToStrike = enemyIntercept.space - enemyCar.car.position
            val strikeVelocity = enemyCarToStrike.scaledToMagnitude(enemyIntercept.accelSlice.speed)
            val impactSpeed = (strikeVelocity - bundle.agentInput.ballVelocity).magnitude()

            val enemyFlatVel = enemyCar.car.velocity.flatten()
            val enemyDribbling = enemyCar.car.position.distance(bundle.agentInput.ballPosition) < 10 &&
                        enemyFlatVel.distance(bundle.agentInput.ballVelocity.flatten()) < 10

            var challengeImminent = false
            val ourIntercept = bundle.tacticalSituation.expectedContact
            val ourTimeToEnemyIntercept = ourIntercept?.distancePlot?.getMotionUponArrival(car, enemyIntercept.space)?.time
            if (ourIntercept != null && ourTimeToEnemyIntercept != null) {
                // Sometimes our own intercept time is not very important because the enemy is going to get there first
                // and redirect the ball.

                challengeImminent = ourIntercept.space.z < 5 &&
                        bundle.tacticalSituation.ballAdvantage.seconds < 0.3 &&
                        ourTimeToEnemyIntercept < Duration.ofSeconds(1.5) &&
                        enemyIntercept.time.isBefore(car.time.plusSeconds(1.0)) &&
                        enemyFlatVel.normalized().dotProduct(enemyToIntercept.normalized()) > 0.5 &&  // Enemy driving toward intercept
                        shotAlignment > 0.6 // Enemy is a threat to our goal
            }


            return ThreatReport(
                    enemyMightBoom = impactSpeed > 30 && shotAlignment > 0.6 && enemyIntercept.space.z < JumpHitStrike.MAX_BALL_HEIGHT_FOR_JUMP_HIT,
                    enemyShotAligned = shotAlignment > 0.6 && enemyIntercept.space.z < JumpHitStrike.MAX_BALL_HEIGHT_FOR_JUMP_HIT,
                    enemyWinsRace = bundle.tacticalSituation.ballAdvantage.millis < 0,
                    enemyHasBreakaway = ZoneUtil.isEnemyOffensiveBreakaway(car, enemyCar.car, ballPosition),
                    ballIsBehindUs = ballIsBehindUs,
                    enemyDribbling = enemyDribbling,
                    challengeImminent = challengeImminent
            )

        }

    }
}
