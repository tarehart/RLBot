package tarehart.rlbot.tuning

import org.junit.Assert
import org.junit.Test

class ManeuverMathTest {
    @Test
    fun secondsForMashJumpHeight() {
        Assert.assertEquals(0.0, ManeuverMath.secondsForMashJumpHeight(ManeuverMath.BASE_CAR_Z).get(), .01)
        Assert.assertEquals(.65, ManeuverMath.secondsForMashJumpHeight(4.5).get(), .01)
        Assert.assertEquals(.81, ManeuverMath.secondsForMashJumpHeight(ManeuverMath.MASH_JUMP_HEIGHT).get(), .01)
        Assert.assertFalse(ManeuverMath.secondsForMashJumpHeight(5.0).isPresent)
    }

}