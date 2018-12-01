package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.integration.metrics.DistanceMetric
import tarehart.rlbot.integration.metrics.VelocityMetric
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.time.Duration

class BallTouchAssert(timeLimit: Duration): PacketAssert(timeLimit, false) {
    override fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?) {

        val touch = bundle.agentInput.latestBallTouch

        if (touch?.playerIndex == bundle.agentInput.playerIndex) {
            status = AssertStatus.SUCCEEDED
        }
    }
}