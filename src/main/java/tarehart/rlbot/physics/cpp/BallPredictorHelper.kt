package tarehart.rlbot.physics.cpp

import com.google.flatbuffers.FlatBufferBuilder
import rlbot.flat.BallPrediction
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import java.nio.ByteBuffer


object BallPredictorHelper {

    fun getV3Offset(builder: FlatBufferBuilder, v: Vector3): Int {
        // Invert the X value so that the axes make more sense.
        return rlbot.flat.Vector3.createVector3(builder,
                (-v.x * Vector3.PACKET_DISTANCE_TO_CLASSIC).toFloat(),
                (v.y * Vector3.PACKET_DISTANCE_TO_CLASSIC).toFloat(),
                (v.z * Vector3.PACKET_DISTANCE_TO_CLASSIC).toFloat())
    }

    fun predictPath(startingSlice: BallSlice, minSeconds: Float): BallPath {

        val builder = FlatBufferBuilder(50)

        rlbot.flat.Physics.startPhysics(builder)
        rlbot.flat.Physics.addLocation(builder, getV3Offset(builder, startingSlice.space))
        rlbot.flat.Physics.addVelocity(builder, getV3Offset(builder, startingSlice.velocity))
        rlbot.flat.Physics.addRotation(builder, getV3Offset(builder, Vector3()))
        rlbot.flat.Physics.addAngularVelocity(builder, getV3Offset(builder, startingSlice.spin))
        val physOffset = rlbot.flat.Physics.endPhysics(builder)

        val sliceOffset = rlbot.flat.PredictionSlice.createPredictionSlice(
                builder,
                startingSlice.time.toSeconds(),
                physOffset)

        builder.finish(sliceOffset)

        val protoBytes = builder.sizedByteArray()

        val rawPath = NativeBallPredictor.predictPath(protoBytes, minSeconds)

        val ballPrediction = BallPrediction.getRootAsBallPrediction(ByteBuffer.wrap(rawPath))

        return BallPath(startingSlice, ballPrediction)
    }
}