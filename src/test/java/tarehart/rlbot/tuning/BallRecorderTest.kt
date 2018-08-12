package tarehart.rlbot.tuning


import com.google.gson.Gson
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.GameTime
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@Ignore
class BallRecorderTest {

    @Test
    @Throws(IOException::class)
    fun testFileOutput() {

        val now = GameTime.zero()
        BallRecorder.startRecording(BallSlice(Vector3(0.0, 0.0, 0.0), now, Vector3(1.0, 0.0, 1.0)), now.plusSeconds(1.0))
        BallRecorder.recordPosition(BallSlice(Vector3(1.0, 1.0, 2.0), now.plusSeconds(1.0), Vector3(1.0, 0.0, 1.0)))
        BallRecorder.recordPosition(BallSlice(Vector3(2.0, 2.0, 5.0), now.plusSeconds(2.0), Vector3(1.0, 0.0, 1.0)))


        val list = Files.list(Paths.get("./" + BallRecorder.DIRECTORY))
        val path = list.sorted().findFirst().get()
        val json = Files.readAllLines(path)[0]
        val ballPath = Gson().fromJson(json, BallPath::class.java)
        Assert.assertEquals(2.0, ballPath.endpoint.space.z, 0.0001)

    }


}