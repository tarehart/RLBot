package tarehart.rlbot.tuning

import com.google.gson.Gson
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.GameTime
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

object BallRecorder {

    const val DIRECTORY = "ballpath"

    // This is going to be an actual ballpath, not predicted.
    private var ballPath: BallPath? = null
    private var endTime: GameTime? = null
    private val gson = Gson()

    fun startRecording(startPoint: BallSlice, endTime: GameTime) {

        if (ballPath == null) {
            ballPath = BallPath(startPoint)
            BallRecorder.endTime = endTime
        }
    }

    fun recordPosition(ballPosition: BallSlice) {
        if (ballPath != null) {

            if (ballPosition.time.isAfter(endTime!!)) {
                // Write to a file
                val path = Paths.get("./" + DIRECTORY + "/" + endTime!!.toMillis() + ".json")
                try {
                    Files.write(path, gson.toJson(ballPath).toByteArray())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                ballPath = null
            } else {
                ballPath!!.addSlice(ballPosition)
            }
        }
    }

}
