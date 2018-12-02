package tarehart.rlbot.intercept.strike

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.time.Duration

class CustomStrike(
        override val preDodgeTime: Duration,
        override val postDodgeTime: Duration,
        override val speedBoost: Double,
        override val style: Style,
        private val verticallyAccessible: (CarData, SpaceTime) -> Boolean = { _, _ -> false }): StrikeProfile() {


    override fun getPlan(car: CarData, intercept: SpaceTime): Plan? {
        return null
    }

    override fun isVerticallyAccessible(car: CarData, intercept: SpaceTime): Boolean {
        return verticallyAccessible.invoke(car, intercept)
    }
}