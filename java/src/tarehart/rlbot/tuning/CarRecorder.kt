package tarehart.rlbot.tuning

import com.google.gson.Gson
import tarehart.rlbot.carpredict.CarPath
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.time.GameTime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CarRecorder(private val endTime: GameTime) {

    private val directory = "carpath"

    // This is going to be an actual ballpath, not predicted.
    private val carPath: CarPath = CarPath()
    private val gson = Gson()

    var open = true

    fun recordPosition(carSlice: CarSlice) {

        if (carSlice.time.isAfter(endTime)) {
            // Write to a file
            val path = Paths.get("./" + directory + "/" + endTime.toMillis() + ".json")
            try {
                Files.write(path, gson.toJson(carPath).toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }

            open = false
        } else {
            carPath.addSlice(carSlice)
        }
    }

}
