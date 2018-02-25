package tarehart.rlbot.carpredict

import tarehart.rlbot.input.CarOrientation
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

data class CarSlice(val space: Vector3, val time: GameTime, val velocity: Vector3, val orientation: CarOrientation) {

    fun toSpaceTime(): SpaceTime {
        return SpaceTime(space, time)
    }
}
