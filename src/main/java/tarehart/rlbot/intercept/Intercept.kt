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
        /**
         * space represents the car position at contact. For the ball position, see ballSlice.
         */
        val space: Vector3,
        val time: GameTime,
        val airBoost: Float,
        val strikeProfile: StrikeProfile, // TODO: consider removing this. Sometimes we want to defer the strikeProfile selection until later.
        val distancePlot: DistancePlot,

        /**
         * When we compute an intercept, once we find ball slices that are in horizontal range of the car,
         * we start testing each ball slice by additional criteria (the spatial predicate). The exact
         * logic is up to the caller, but common examples are checking whether the ball is too high, or
         * whether it is too far to the side of the goal for a shot, etc. If there are spatial predicate
         * failures before we finally found a good intercept, this is an indication that you probably
         * don't want to arrive early.
         */
        val spatialPredicateFailurePeriod: Duration,
        val ballSlice: BallSlice,
        val accelSlice: DistanceTimeSpeed) {

    fun toSpaceTime(): SpaceTime {
        return SpaceTime(space, time)
    }

    val needsPatience = spatialPredicateFailurePeriod.millis > 0
}
