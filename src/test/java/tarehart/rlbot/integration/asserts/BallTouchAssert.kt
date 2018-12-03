package tarehart.rlbot.integration.asserts

import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.bots.Team
import tarehart.rlbot.integration.metrics.DistanceMetric
import tarehart.rlbot.integration.metrics.VelocityMetric
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.physics.BallPhysics
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
            val CAR_MASS = 180.0
            println("Before velocity; Car: ${previousBundle!!.agentInput.myCarData.velocity}, Ball: ${previousBundle.agentInput.ballVelocity}")
            println("After velocity; Car: ${bundle.agentInput.myCarData.velocity}, Ball: ${bundle.agentInput.ballVelocity}")
            val car_before = previousBundle.agentInput.myCarData.velocity
            val ball_before = previousBundle.agentInput.ballVelocity
            val car_after = bundle.agentInput.myCarData.velocity
            var ball_after = bundle.agentInput.ballVelocity

            val impulse = BallPhysics.calculateBallImpactForce(
                    previousBundle.agentInput.myCarData.position,
                    previousBundle.agentInput.myCarData.velocity,
                    previousBundle.agentInput.ballPosition,
                    previousBundle.agentInput.ballVelocity,
                    previousBundle.agentInput.myCarData.orientation.noseVector)

            ball_after -= impulse

            // ball_mass = car_mass * (car_vel_after - car_vel_before) / (ball_vel_after - ball_vel_before)
            val ball_mass = ((car_after - car_before) / (ball_after - ball_before)) * CAR_MASS
            println("Ball mass; ${ball_mass}")
            println("Car rotation before: ${previousBundle.agentInput.myCarData.spin.angularVelGlobal}")
            println("Car rotation after: ${bundle.agentInput.myCarData.spin.angularVelGlobal}")

            println("Ball Impulse: ${impulse}")
            status = AssertStatus.SUCCEEDED
        }

    }
}