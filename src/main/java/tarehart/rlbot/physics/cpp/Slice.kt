package tarehart.rlbot.physics.cpp

import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.time.GameTime


class Slice(val location: FloatArray, val velocity: FloatArray, val angularVelocity: FloatArray, val time: Float) {

    fun toBallSlice(): BallSlice {
        return BallSlice(
                toVector3(location),
                GameTime.fromGameSeconds(time.toDouble()),
                toVector3(velocity),
                toVector3(angularVelocity))
    }

    companion object {

        fun fromVector3(v3: Vector3): FloatArray {
            return floatArrayOf(v3.x.toFloat(), v3.y.toFloat(), v3.z.toFloat())
        }

        fun toVector3(floats: FloatArray): Vector3 {
            return Vector3(floats[0].toDouble(), floats[1].toDouble(), floats[2].toDouble())
        }

        fun fromBallSlice(ballSlice: BallSlice): Slice {
            return Slice(
                    fromVector3(ballSlice.space),
                    fromVector3(ballSlice.velocity),
                    fromVector3(ballSlice.spin),
                    ballSlice.time.toSeconds())
        }
    }
}


