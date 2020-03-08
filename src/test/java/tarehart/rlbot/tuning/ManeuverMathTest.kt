package tarehart.rlbot.tuning

import org.junit.Assert
import org.junit.Test

class ManeuverMathTest {
    @Test
    fun secondsForMashJumpHeight() {
        val delta = .01F
        Assert.assertEquals(0F, ManeuverMath.secondsForMashJumpHeight(ManeuverMath.BASE_CAR_Z)!!, delta)
        Assert.assertEquals(.65F, ManeuverMath.secondsForMashJumpHeight(4.5F)!!, delta)
        Assert.assertEquals(.81F, ManeuverMath.secondsForMashJumpHeight(ManeuverMath.MASH_JUMP_HEIGHT)!!, delta)
        Assert.assertNull(ManeuverMath.secondsForMashJumpHeight(5F))
    }

}
