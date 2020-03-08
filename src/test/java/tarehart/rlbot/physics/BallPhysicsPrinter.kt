package tarehart.rlbot.physics

import org.junit.Test
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarHitbox
import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath
import kotlin.math.asin
import kotlin.math.cos

class BallPhysicsPrinter {
    @Test
    fun print() {

        // Actual (0.00, 45.61, 13.58)
        // Predicted Ball final velocity: (-0.00, 42.18, 16.45)

        val carPos = Vector3(0, -3.15, ManeuverMath.BASE_CAR_Z)
        val carVel = Vector3(0, 34.91, 0)
        val inelasticImpulse = BallPhysics.calculateCarBallCollisionImpulse(
                carPosition = carPos,
                carVelocity = carVel,
                ballPosition = Vector3(0, 0, ArenaModel.BALL_RADIUS),
                ballVelocity = Vector3())

        val scriptImpulse = BallPhysics.calculateScriptBallImpactForce(
                carPosition = carPos,
                carVelocity = carVel,
                ballPosition = Vector3(0, 0, ArenaModel.BALL_RADIUS),
                ballVelocity = Vector3(),
                carForwardDirectionNormal = Vector3(0, 1, 0))

        print(inelasticImpulse / 30.0 + scriptImpulse)
    }

    @Test
    fun printChipOptions() {
        val options = BallPhysics.computeChipOptions(
                currentCarPosition = Vector3(0, -50, 0),
                arrivalSpeed = 35.0,
                ballSlice = BallSlice(
                        space = Vector3(0, 0, ArenaModel.BALL_RADIUS),
                        time = GameTime.zero(),
                        velocity = Vector3.ZERO,
                        spin = Vector3.ZERO),
                hitbox = CarHitbox.OCTANE)

        print(options)
    }


}
