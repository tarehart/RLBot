package tarehart.rlbot.integration.asserts

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.BallState
import rlbot.gamestate.DesiredVector3
import rlbot.gamestate.GameState
import rlbot.gamestate.PhysicsState
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.physics.BallPhysics
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class BallTouchAssert(timeLimit: Duration): PacketAssert(timeLimit, false) {

    lateinit var firstCheckTime: GameTime

    override fun checkBundle(bundle: TacticalBundle, previousBundle: TacticalBundle?) {

        if (! ::firstCheckTime.isInitialized) { // I actually want start time, any reason that the asserts don't get that?
            firstCheckTime = bundle.agentInput.time
        }

        val touch = bundle.agentInput.latestBallTouch

        if (touch?.playerIndex == bundle.agentInput.playerIndex && touch.time > firstCheckTime) {
            val CAR_MASS = 180.0
            println("Before velocity; Car: ${previousBundle!!.agentInput.myCarData.velocity}, Ball: ${previousBundle.agentInput.ballVelocity}")
            println("After velocity; Car: ${bundle.agentInput.myCarData.velocity}, Ball: ${bundle.agentInput.ballVelocity}")
            val car_before = previousBundle.agentInput.myCarData.velocity
            val ball_before = previousBundle.agentInput.ballVelocity
            val car_after = bundle.agentInput.myCarData.velocity
            var ball_after = bundle.agentInput.ballVelocity


            val impulse = BallPhysics.calculateScriptBallImpactForce(
                    previousBundle.agentInput.myCarData.position,
                    previousBundle.agentInput.myCarData.velocity,
                    previousBundle.agentInput.ballPosition,
                    previousBundle.agentInput.ballVelocity,
                    previousBundle.agentInput.myCarData.orientation.noseVector)

            val physicsImpulse = BallPhysics.calculateCarBallCollisionImpulse(
                    previousBundle.agentInput.myCarData.position,
                    previousBundle.agentInput.myCarData.velocity,
                    previousBundle.agentInput.ballPosition,
                    previousBundle.agentInput.ballVelocity
                    )

            ball_after -= impulse
            println("Ball velocity after, without script: ${ball_after}")
            val delta_before = car_before - ball_before
            val delta_after = car_after - ball_after

            // ball_mass = car_mass * (car_vel_after - car_vel_before) / (ball_vel_after - ball_vel_before)
            val ball_mass = ((car_after - car_before) / (ball_after - ball_before)) * CAR_MASS
            println("Ball mass; ${ball_mass}")
            println("Car rotation before: ${previousBundle.agentInput.myCarData.spin.angularVelGlobal}")
            println("Car rotation after: ${bundle.agentInput.myCarData.spin.angularVelGlobal}")

            println("Ball Script Delta V: ${impulse}")
            println("Physical Impulse: ${physicsImpulse}")
            println("Ball physical change in velocity: ${physicsImpulse / 30.0}")
            println("Car physical change in velocity: ${physicsImpulse / 180.0}")
            println("Ball change in velocity: ${bundle.agentInput.ballVelocity - previousBundle.agentInput.ballVelocity}")

            println("Relative Velocity; before: ${delta_before}, after: ${delta_after}")
            println("CoR: ${delta_after / delta_before}")

            println("-----------------")
            println("Predicted Car final velocity: ${car_before + physicsImpulse / -180.0}")
            println("Predicted Ball final velocity: ${ball_before + physicsImpulse / 30.0 + impulse}")
            status = AssertStatus.SUCCEEDED
        }

    }
}
