package tarehart.rlbot.tuning

import com.google.gson.Gson
import org.junit.Assert
import org.junit.Test
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.Duration

import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.util.ArrayList
import java.util.Optional
import java.util.Scanner

class PredictedVsActualTest {
    private val arenaModel = ArenaModel()


    private fun readRecording(filename: String): BallPath {
        val input = javaClass.getResourceAsStream("/ballrecordings/" + filename)
        val s = Scanner(input).useDelimiter("\\A")
        val content = if (s.hasNext()) s.next() else ""

        val gson = Gson()
        return gson.fromJson(content, BallPath::class.java)
    }


    // These files were deleted because the format became obsolete. We should record some new ones.

    //    @Test
    //    public void spinlessBackwall() throws UnsupportedEncodingException {
    //        testFile("spinless-backwall.json");
    //    }
    //
    //    @Test
    //    public void spinlessGround() throws UnsupportedEncodingException {
    //        testFile("spinless-ground.json");
    //    }

    @Throws(UnsupportedEncodingException::class)
    private fun testFile(filename: String) {

        var actualPath = readRecording(filename)
        actualPath = finesseActualPath(actualPath)
        val predictedPath = makePrediction(actualPath)
        // (-73.29997, 65.447556, 4.5342107) after first time step

        val actual = actualPath.slices
        for (i in actual.indices) {
            if (i < 20) {
                val velocity = actual[i].velocity
                val speedNow = velocity.magnitude()

                val velocityAfter = actual[i + 1].velocity
                val speedNext = velocityAfter.magnitude()

                val drag = speedNext / speedNow
                val dragPerSpeed = drag / speedNow
                //System.out.println(String.format("Velocity: %s Speed: %s Drag: %s DragPerSpeed: %s", velocity, speedNow, drag, dragPerSpeed));
            }
        }

        val predictedSlices = predictedPath.slices

        val actualTrimmed = ArrayList<BallSlice>(predictedSlices.size)

        for (i in 0 until predictedSlices.size - 1) {
            val actualSlice = actualPath.getMotionAt(predictedSlices[i].time)!!
            actualTrimmed.add(actualSlice)
            println(String.format("A: %s\nP: %s\n", actualSlice, predictedSlices[i]))

        }
        actualTrimmed.add(actualPath.endpoint)

        for (i in predictedSlices.indices) {

            val actualSlice = actualTrimmed[i].space
            val (x, y) = predictedSlices[i].space.minus(actualSlice)
            val error = Vector2(x, y).magnitude()
            if (error > THRESHOLD) {
                val duration = Duration.between(actualTrimmed[0].time, actualTrimmed[i].time)
                Assert.fail(String.format("Diverged to %.2f after %.2f seconds!", error, duration.seconds))
            }
        }
    }

    private fun finesseActualPath(actualPath: BallPath): BallPath {
        val newStart = actualPath.getMotionAt(actualPath.startPoint.time.plus(Duration.ofMillis(100)))
        val finessed = BallPath(newStart!!)
        val slices = actualPath.slices
        for (i in 0 until actualPath.slices.size) {
            if (slices[i].time.isAfter(newStart.time)) {
                finessed.addSlice(slices[i])
            }
        }
        return finessed
    }

    private fun makePrediction(backWallActual: BallPath): BallPath {
        val duration = Duration.between(backWallActual.startPoint.time, backWallActual.endpoint.time)
        return arenaModel.simulateBall(backWallActual.startPoint, duration)
    }

    companion object {


        private val THRESHOLD = 4.0
    }

}
