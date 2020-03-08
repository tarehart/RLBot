package tarehart.rlbot.tuning

import com.google.gson.Gson
import rlbot.render.NamedRenderer
import tarehart.rlbot.bots.Team
import tarehart.rlbot.carpredict.AccelerationModel
import tarehart.rlbot.carpredict.CarPath
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.input.CarData
import tarehart.rlbot.input.CarHitbox
import tarehart.rlbot.input.CarSpin
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import java.util.*

class CarPathHelper {

    fun readRecording(filename: String): CarPath {
        val streamIn = javaClass.getResourceAsStream("/tst/carrecordings/$filename")
        val s = Scanner(streamIn).useDelimiter("\\A")
        val content = if (s.hasNext()) s.next() else ""

        val gson = Gson()
        return gson.fromJson(content, CarPath::class.java)
    }

    fun makePrediction(actual: CarPath, hasBoost: Boolean): DistancePlot {
        val duration = Duration.between(actual.firstSlice.time, actual.lastSlice.time)
        val carData = getCarDataFromSlice(actual.firstSlice)
        return AccelerationModel.simulateAcceleration(carData, duration, (if (hasBoost) 100 else 0).toFloat(), 0.0)
    }

    private fun getCarDataFromSlice(carSlice: CarSlice): CarData {
        return CarData(
                carSlice.space,
                carSlice.velocity,
                carSlice.orientation,
                CarSpin(0F, 0F, 0F, Vector3()),
                100F,
                false,
                Team.BLUE,
                0,
                carSlice.time,
                0,
                true,
                false,
                "",
                NamedRenderer("test"),
                isBot = true,
                hitbox = CarHitbox.OCTANE)
    }

}
