package tarehart.rlbot.tuning

import org.junit.Assert
import org.junit.Test
import tarehart.rlbot.carpredict.CarPath
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

import java.io.UnsupportedEncodingException
import java.util.Optional

class AccelerationPredictedVsActualTest {

    private val carPathHelper = CarPathHelper()

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun fullBoost() {
        testFile("fullBoostAccel.json", true)
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun zeroBoost() {
        testFile("zeroBoostAccel.json", false)
    }


    @Throws(UnsupportedEncodingException::class)
    private fun testFile(filename: String, hasBoost: Boolean) {

        val actualPath = carPathHelper.readRecording(filename)
        val predictedAccel = carPathHelper.makePrediction(actualPath, hasBoost)
        // (-73.29997, 65.447556, 4.5342107) after first time step

        val actual = actualPath.path
        val startTime = actual[0].time

        for (slice in actual) {
            val predicted = predictedAccel.getMotionAfterDuration(Duration.between(startTime, slice.time))
            predicted ?: throw AssertionError()
            Assert.assertEquals(predicted.speed, slice.velocity.magnitude(), 1F)
        }
    }
}
