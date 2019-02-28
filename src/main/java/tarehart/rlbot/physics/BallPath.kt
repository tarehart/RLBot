package tarehart.rlbot.physics

import rlbot.flat.BallPrediction
import rlbot.flat.Bool
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.GoalUtil
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import java.util.*

class BallPath() {

    val slices = ArrayList<BallSlice>()

    val startPoint: BallSlice
        get() = this.slices[0]

    val endpoint: BallSlice
        get() = this.slices[this.slices.size - 1]

    constructor(prediction: BallPrediction, endAtGoal: Boolean = false): this() {

        var scoreLocation: Vector3? = null

        for (i in 0 until prediction.slicesLength()) {
            val slice = prediction.slices(i)
            val physics = slice.physics()

            val location = Vector3.fromRlbot(physics.location())

            if (endAtGoal && i > 0) {
                val scorePlane = GoalUtil.getNearestGoal(location).scorePlane
                val planeBreak = getPlaneBreak(slices[i - 1].space, location, scorePlane)
                if (planeBreak != null) {
                    // Set the score location to a point that's actually past the plane
                    // so that the constructed ball path will definitely register
                    // a plane break again if we query it.
                    scoreLocation = location
                }
            }

            slices.add(BallSlice(
                    location,
                    GameTime.fromGameSeconds(slice.gameSeconds().toDouble()),
                    Vector3.fromRlbot(physics.velocity()),
                    Vector3.fromRlbot(physics.angularVelocity())))

            if (scoreLocation != null) {
                break
            }
        }

        if (scoreLocation != null) {
            // Add a dummy slice timed at the end of the prediction.
            slices.add(BallSlice(
                    scoreLocation,
                    GameTime.fromGameSeconds(prediction.slices(prediction.slicesLength() - 1).gameSeconds().toDouble()),
                    Vector3.ZERO,
                    Vector3.ZERO))
        }
    }

    constructor(startingSlice: BallSlice): this() {
        slices.add(startingSlice)
    }

    fun addSlice(spaceTime: BallSlice) {
        this.slices.add(spaceTime)
    }

    fun getMotionAt(time: GameTime): BallSlice? {
        if (time.isBefore(this.slices[0].time) || time.isAfter(this.slices[this.slices.size - 1].time)) {
            return null
        }

        for (i in 0 until this.slices.size - 1) {
            val (space1, time1, velocity1) = this.slices[i]
            val (space2, time2, velocity2, spin) = this.slices[i + 1]
            if (time2.isAfter(time)) {

                val simulationStepMillis = Duration.between(time1, time2).millis
                val tweenPoint = Duration.between(time1, time).millis * 1.0 / simulationStepMillis
                val toNext = space2.minus(space1)
                val toTween = toNext.scaled(tweenPoint)
                val space = space1.plus(toTween)
                val velocity = VectorUtil.weightedAverage(velocity1, velocity2, 1 - tweenPoint)
                return BallSlice(space, time, velocity, spin)
            }
        }

        return endpoint
    }

    /**
     * Bounce counting starts at 1.
     *
     * 0 is not a valid input.
     */
    fun getMotionAfterWallBounce(targetBounce: Int): BallSlice? {

        assert(targetBounce > 0)

        var numBounces = 0

        for (i in 1 until this.slices.size) {
            val spt = this.slices[i]
            val (_, _, velocity) = this.slices[i - 1]

            if (isWallBounce(velocity, spt.velocity)) {
                numBounces++
            }

            if (numBounces == targetBounce) {
                return if (this.slices.size == i + 1) {
                    null
                } else spt
            }
        }

        return null
    }

    private fun isWallBounce(previousVelocity: Vector3, currentVelocity: Vector3): Boolean {
        if (currentVelocity.magnitudeSquared() < .01) {
            return false
        }
        val prev = previousVelocity.flatten()
        val curr = currentVelocity.flatten()

        return Vector2.angle(prev, curr) > Math.PI / 6
    }

    private fun isFloorBounce(previousVelocity: Vector3, currentVelocity: Vector3): Boolean {
        return previousVelocity.z < 0 && currentVelocity.z > 0
    }

    fun getLanding(startOfSearch: GameTime): BallSlice? {

        for (i in 1 until this.slices.size) {
            val (space, time, velocity, spin) = this.slices[i]

            if (time.isBefore(startOfSearch)) {
                continue
            }

            val (space1, time1, velocity1) = this.slices[i - 1]


            if (isFloorBounce(velocity1, velocity)) {
                if (this.slices.size == i + 1) {
                    return null
                }

                val floorGapOfPrev = space1.z - ArenaModel.BALL_RADIUS
                val floorGapOfCurrent = space.z - ArenaModel.BALL_RADIUS

                var bouncePosition = BallSlice(
                        Vector3(space.x, space.y, ArenaModel.BALL_RADIUS.toDouble()),
                        time,
                        velocity,
                        spin)
                if (floorGapOfPrev < floorGapOfCurrent) {
                    // TODO: consider interpolating instead of just picking the more accurate.
                    bouncePosition = BallSlice(
                            Vector3(space1.x, space1.y, ArenaModel.BALL_RADIUS.toDouble()),
                            time1,
                            velocity,
                            spin)
                }

                return bouncePosition
            }
        }

        return null
    }

    fun getPlaneBreak(searchStart: GameTime, plane: Plane, directionSensitive: Boolean,
                      spacePredicate: (Vector3) -> Boolean = { true }): BallSlice? {

        for (i in 1 until this.slices.size) {
            val currSlice = this.slices[i]

            if (currSlice.time.isBefore(searchStart)) {
                continue
            }

            val prevSlice = this.slices[i - 1]

            if (directionSensitive && currSlice.space.minus(prevSlice.space).dotProduct(plane.normal) > 0) {
                // Moving the same direction as the plane normal. If we're direction sensitive, then we don't care about plane breaks in this direction.
                continue
            }

            getPlaneBreak(prevSlice.space, currSlice.space, plane)?.let {
                if (spacePredicate(it)) {
                    val stepSeconds = Duration.between(prevSlice.time, currSlice.time).seconds
                    val tweenPoint = prevSlice.space.distance(it) / prevSlice.space.distance(currSlice.space)
                    val moment = prevSlice.time.plusSeconds(stepSeconds * tweenPoint)
                    val velocity = VectorUtil.weightedAverage(prevSlice.velocity, currSlice.velocity, 1 - tweenPoint)
                    return BallSlice(it, moment, velocity, currSlice.spin)
                }
            }
        }

        return null
    }

    private fun getPlaneBreak(start: Vector3, end: Vector3, plane: Plane): Vector3? {
        return VectorUtil.getPlaneIntersection(plane, start, end.minus(start))
    }

    fun findSlice(decider: (BallSlice) -> Boolean): BallSlice? {
        return this.slices.drop(1).firstOrNull { decider.invoke(it) }
    }

    fun startingFrom(earliestIntercept: GameTime): BallPath? {
        throw RuntimeException("startingFrom is deprecated.")
    }
}
