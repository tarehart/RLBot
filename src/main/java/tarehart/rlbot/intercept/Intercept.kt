package tarehart.rlbot.intercept

import tarehart.rlbot.intercept.strike.StrikeProfile
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

data class Intercept(
        val space: Vector3,
        val time: GameTime,
        val airBoost: Double,
        val strikeProfile: StrikeProfile, // TODO: consider removing this. Sometimes we want to defer the strikeProfile selection until later.
        val distancePlot: DistancePlot,
        val spareTime: Duration,
        val ballSlice: BallSlice,
        val accelSlice: DistanceTimeSpeed) {

    fun toSpaceTime(): SpaceTime {
        return SpaceTime(space, time)
    }
}
