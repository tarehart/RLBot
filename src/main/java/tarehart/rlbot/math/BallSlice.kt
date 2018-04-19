package tarehart.rlbot.math

import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime

// Spin is expressed as the axis of rotation. The magnitude represents the rate.
// If the magnitude is 2pi, then it rotates once per second.
data class BallSlice(val space: Vector3, val time: GameTime, val velocity: Vector3, val spin: Vector3 = Vector3()) {

    @Deprecated("")
    constructor(space: Vector3, time: GameTime, velocity: Vector3) : this(space, time, velocity, Vector3())

    fun toSpaceTime(): SpaceTime {
        return SpaceTime(space, time)
    }

    override fun toString(): String {
        return "BallSlice{" +
                "space=" + space +
                ", time=" + time +
                ", velocity=" + velocity +
                ", spin=" + spin +
                '}'
    }
}
