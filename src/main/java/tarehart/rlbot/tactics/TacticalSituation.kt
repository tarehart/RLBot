package tarehart.rlbot.tactics

import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.planning.CarWithIntercept
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.time.Duration

class TacticalSituation(
        val ownGoalFutureProximity: Double,
        val distanceBallIsBehindUs: Double,
        val enemyOffensiveApproachError: Double? = null, // If the enemy wants to shoot on our goal, how many radians away from a direct approach? Always positive.
        val expectedContact: Intercept? = null,
        val expectedEnemyContact: Intercept? = null,
        val ballAdvantage: Duration,
        val scoredOnThreat: BallSlice? = null,
        val needsDefensiveClear: Boolean,
        val shotOnGoalAvailable: Boolean,
        val goForKickoff: Boolean,
        val currentPlan: Plan? = null,
        val futureBallMotion: BallSlice? = null,
        val enemyPlayerWithInitiative: CarWithIntercept?,
        val teamPlayerWithInitiative: CarWithIntercept,
        val teamPlayerWithBestShot: CarWithIntercept,
        val ballPath: BallPath,
        val gameMode: GameMode
)
