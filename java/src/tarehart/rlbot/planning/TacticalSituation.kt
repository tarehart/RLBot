package tarehart.rlbot.planning

import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.BallSlice

import java.util.Optional

class TacticalSituation {

    var ownGoalFutureProximity = 0.0
    var distanceBallIsBehindUs = 0.0
    var enemyOffensiveApproachError: Double? = null // If the enemy wants to shoot on our goal, how many radians away from a direct approach? Always positive.
    var expectedContact: Intercept? = null
    var expectedEnemyContact: Intercept? = null
    var distanceFromEnemyBackWall: Double = 0.0
    var distanceFromEnemyCorner = 0.0
    var scoredOnThreat: BallSlice? = null
    var needsDefensiveClear = false
    var shotOnGoalAvailable = false
    var forceDefensivePosture = false
    var goForKickoff = false
    var waitToClear = false
    var currentPlan: Plan? = null
    var futureBallMotion: BallSlice? = null
}
