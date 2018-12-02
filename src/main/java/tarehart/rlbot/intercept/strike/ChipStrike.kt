package tarehart.rlbot.intercept.strike

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.time.Duration

class ChipStrike: StrikeProfile() {

    override val preDodgeTime = Duration.ZERO
    override val speedBoost = 0.0
    override val postDodgeTime = Duration.ZERO
    override val style = Style.CHIP

    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return intercept.space.z <= MAX_HEIGHT_OF_BALL_FOR_CHIP
    }

    companion object {
        const val MAX_HEIGHT_OF_BALL_FOR_CHIP = 2.0
    }
}