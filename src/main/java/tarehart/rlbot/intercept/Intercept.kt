package tarehart.rlbot.intercept

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

data class Intercept(
        val space: Vector3,
        val time: GameTime,
        val airBoost: Double,
        val strikeProfile: StrikeProfile,
        val distancePlot: DistancePlot,
        val spareTime: Duration,
        val ballSlice: BallSlice) {

    fun toSpaceTime(): SpaceTime {
        return SpaceTime(space, time)
    }
}
