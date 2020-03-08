package tarehart.rlbot.physics

import org.junit.Test

import org.junit.Assert.*
import tarehart.rlbot.input.CarHitbox
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath

class BallPhysicsTest {

    @Test
    fun computeChipOptions() {

        val chipOptions = BallPhysics.computeChipOptions(
                Vector3(0, -50, ManeuverMath.BASE_CAR_Z),
                40F,
                BallSlice(Vector3(0, 0, 0), GameTime.zero(), Vector3(), Vector3()),
                CarHitbox.OCTANE)

    }
}
