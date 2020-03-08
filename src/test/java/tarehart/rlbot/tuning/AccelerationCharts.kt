package tarehart.rlbot.tuning

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.data.xy.DefaultXYDataset
import tarehart.rlbot.carpredict.CarPath
import tarehart.rlbot.carpredict.CarSlice
import tarehart.rlbot.math.DistanceTimeSpeed
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.time.Duration

import javax.swing.*
import java.io.UnsupportedEncodingException
import java.util.ArrayList
import java.util.Optional

class AccelerationCharts {

    private val carPathHelper = CarPathHelper()

    @Throws(UnsupportedEncodingException::class)
    fun chartZeroBoost() {
        chartFile("zeroBoostAccel.json", false)
    }

    @Throws(UnsupportedEncodingException::class)
    fun chartFullBoost() {
        chartFile("fullBoostAccel.json", true)
    }

    private fun chartFile(filename: String, hasBoost: Boolean) {
        val actualPath = carPathHelper.readRecording(filename)
        val predictedAccel = carPathHelper.makePrediction(actualPath, hasBoost)

        val vel = VelocityChart(predictedAccel, actualPath)
        vel.pack()
        vel.isVisible = true

        val distance = DistanceChart(predictedAccel, actualPath)
        distance.pack()
        distance.isVisible = true
    }

    private abstract inner class AccelChart(predicted: DistancePlot, actual: CarPath) : JFrame("Distance Diff") {

        protected var start: CarSlice

        protected abstract val yLabel: String

        init {

            start = actual.path[0]

            val dataset = DefaultXYDataset()
            dataset.addSeries("predicted", getPredictedDataSeries(predicted, .1))
            dataset.addSeries("actual", getActualDataSeries(actual))

            val chart = createChart(dataset, "Acceleration Diff")

            val chartPanel = ChartPanel(chart)
            chartPanel.preferredSize = java.awt.Dimension(1500, 800)
            // add it to our application
            contentPane = chartPanel
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        }

        private fun getPredictedDataSeries(predicted: DistancePlot, timeStep: Double): Array<DoubleArray> {

            val totalSeconds = predicted.slices[predicted.slices.size - 1].time.seconds

            val xValues = ArrayList<Double>()
            val yValues = ArrayList<Double>()

            var secs = 0.0
            while (secs < totalSeconds) {
                val motion = predicted.getMotionAfterDuration(Duration.ofSeconds(secs))
                xValues.add(secs)
                yValues.add(getYValue(motion))
                secs += timeStep
            }

            return arrayOf(xValues.stream().mapToDouble { d -> d }.toArray(), yValues.stream().mapToDouble { d -> d }.toArray())
        }

        private fun getActualDataSeries(actual: CarPath): Array<DoubleArray> {

            val xValues = ArrayList<Double>()
            val yValues = ArrayList<Double>()

            val (_, time) = actual.path[0]

            for (slice in actual.path) {
                xValues.add(Duration.between(time, slice.time).seconds.toDouble())
                yValues.add(getYValue(slice))
            }

            return arrayOf(xValues.stream().mapToDouble { d -> d.toDouble() }.toArray(), yValues.stream().mapToDouble { d -> d.toDouble() }.toArray())
        }

        private fun createChart(dataset: DefaultXYDataset, title: String): JFreeChart {
            return ChartFactory.createXYLineChart(title, "time", yLabel, dataset)
        }

        protected abstract fun getYValue(motion: DistanceTimeSpeed?): Double

        protected abstract fun getYValue(slice: CarSlice): Double
    }

    private inner class DistanceChart(predicted: DistancePlot, actual: CarPath) : AccelChart(predicted, actual) {

        override val yLabel: String
            get() = "distance"

        override fun getYValue(slice: CarSlice): Double {
            return slice.space.distance(start.space).toDouble()
        }

        override fun getYValue(motion: DistanceTimeSpeed?): Double {
            return motion!!.distance.toDouble()
        }
    }


    private inner class VelocityChart(predicted: DistancePlot, actual: CarPath) : AccelChart(predicted, actual) {

        override val yLabel: String
            get() = "velocity"

        override fun getYValue(motion: DistanceTimeSpeed?): Double {
            return motion!!.speed.toDouble()
        }

        override fun getYValue(slice: CarSlice): Double {
            return slice.velocity.magnitude().toDouble()
        }
    }

    companion object {

        @Throws(UnsupportedEncodingException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val accelerationCharts = AccelerationCharts()
            //accelerationCharts.chartZeroBoost();
            accelerationCharts.chartFullBoost()
        }
    }

}
