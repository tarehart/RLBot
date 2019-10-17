package tarehart.rlbot.tuning

import org.junit.Assert
import org.junit.Test

class ManeuverMathTest {
    @Test
    fun secondsForMashJumpHeight() {
        Assert.assertEquals(0.0, ManeuverMath.secondsForMashJumpHeight(ManeuverMath.BASE_CAR_Z)!!, .01)
        Assert.assertEquals(.65, ManeuverMath.secondsForMashJumpHeight(4.5)!!, .01)
        Assert.assertEquals(.81, ManeuverMath.secondsForMashJumpHeight(ManeuverMath.MASH_JUMP_HEIGHT)!!, .01)
        Assert.assertNull(ManeuverMath.secondsForMashJumpHeight(5.0))
    }

}
