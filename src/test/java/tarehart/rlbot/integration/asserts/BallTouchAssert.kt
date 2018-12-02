package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.integration.metrics.DistanceMetric
import tarehart.rlbot.integration.metrics.VelocityMetric
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class BallTouchAssert(timeLimit: Duration): PacketAssert(timeLimit, false) {

    lateinit var firstCheckTime: GameTime

    override fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?) {

        if (! ::firstCheckTime.isInitialized) { // I actually want start time, any reason that the asserts don't get that?
            firstCheckTime = bundle.agentInput.time
        }

        val touch = bundle.agentInput.latestBallTouch

        if (touch?.playerIndex == bundle.agentInput.playerIndex && touch?.time > firstCheckTime) {
            status = AssertStatus.SUCCEEDED
        }
    }
}