package tarehart.rlbot.planning

import tarehart.rlbot.input.CarData
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.time.Duration

class TacticalSituation(
        val ownGoalFutureProximity: Double,
        val distanceBallIsBehindUs: Double,
        val enemyOffensiveApproachError: Double? = null, // If the enemy wants to shoot on our goal, how many radians away from a direct approach? Always positive.
        val expectedContact: Intercept? = null,
        val expectedEnemyContact: Intercept? = null,
        val ballAdvantage: Duration,
        val distanceFromEnemyBackWall: Double,
        val distanceFromEnemyCorner: Double,
        val scoredOnThreat: BallSlice? = null,
        val needsDefensiveClear: Boolean,
        val shotOnGoalAvailable: Boolean,
        val forceDefensivePosture: Boolean,
        val goForKickoff: Boolean,
        val waitToClear: Boolean,
        val currentPlan: Plan? = null,
        val futureBallMotion: BallSlice? = null,
        val enemyPlayerWithInitiative: CarWithIntercept?,
        val teamPlayerWithInitiative: CarWithIntercept
)
