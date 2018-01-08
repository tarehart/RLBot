package tarehart.rlbot.tuning;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import tarehart.rlbot.carpredict.CarPath;
import tarehart.rlbot.carpredict.CarSlice;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.time.Duration;

import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccelerationCharts {

    private CarPathHelper carPathHelper = new CarPathHelper();

    public static void main(String[] args) throws UnsupportedEncodingException {
        AccelerationCharts accelerationCharts = new AccelerationCharts();
        //accelerationCharts.chartZeroBoost();
        accelerationCharts.chartFullBoost();
    }

    public void chartZeroBoost() throws UnsupportedEncodingException {
        chartFile("zeroBoostAccel.json", false);
    }

    public void chartFullBoost() throws UnsupportedEncodingException {
        chartFile("fullBoostAccel.json", true);
    }

    private void chartFile(String filename, boolean hasBoost) {
        CarPath actualPath = carPathHelper.readRecording(filename);
        DistancePlot predictedAccel = carPathHelper.makePrediction(actualPath, hasBoost);

        AccelChart vel = new VelocityChart(predictedAccel, actualPath);
        vel.pack();
        vel.setVisible(true);

        AccelChart distance = new DistanceChart(predictedAccel, actualPath);
        distance.pack();
        distance.setVisible(true);
    }

    private abstract class AccelChart extends JFrame {

        protected CarSlice start;

        public AccelChart(DistancePlot predicted, CarPath actual) {
            super("Distance Diff");

            start = actual.getPath().get(0);

            DefaultXYDataset dataset = new DefaultXYDataset();
            dataset.addSeries("predicted", getPredictedDataSeries(predicted, .1));
            dataset.addSeries("actual", getActualDataSeries(actual));

            JFreeChart chart = createChart(dataset, "Acceleration Diff");

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(1500, 800));
            // add it to our application
            setContentPane(chartPanel);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }

        private double[][] getPredictedDataSeries(DistancePlot predicted, double timeStep) {

            double totalSeconds = predicted.getSlices().get(predicted.getSlices().size() - 1).getTime().getSeconds();

            List<Double> xValues = new ArrayList<>();
            List<Double> yValues = new ArrayList<>();

            for (double secs = 0; secs < totalSeconds; secs += timeStep) {
                Optional<DistanceTimeSpeed> motion = predicted.getMotionAfterDuration(Duration.Companion.ofSeconds(secs));
                xValues.add(secs);
                yValues.add(getYValue(motion));
            }

            return new double[][] {
                    xValues.stream().mapToDouble(d -> d).toArray(),
                    yValues.stream().mapToDouble(d -> d).toArray()
            };
        }

        private double[][] getActualDataSeries(CarPath actual) {

            List<Double> xValues = new ArrayList<>();
            List<Double> yValues = new ArrayList<>();

            CarSlice start = actual.getPath().get(0);

            for (CarSlice slice: actual.getPath()) {
                xValues.add(Duration.Companion.between(start.getTime(), slice.getTime()).getSeconds());
                yValues.add(getYValue(slice));
            }

            return new double[][] {
                    xValues.stream().mapToDouble(d -> d).toArray(),
                    yValues.stream().mapToDouble(d -> d).toArray()
            };
        }

        private JFreeChart createChart(DefaultXYDataset dataset, String title) {
            JFreeChart chart = ChartFactory.createXYLineChart(title, "time", getYLabel(), dataset);
            return chart;
        }

        protected abstract String getYLabel();

        protected abstract double getYValue(Optional<DistanceTimeSpeed> motion);

        protected abstract double getYValue(CarSlice slice);
    }

    private class DistanceChart extends AccelChart {

        public DistanceChart(DistancePlot predicted, CarPath actual) {
            super(predicted, actual);
        }

        @Override
        protected double getYValue(CarSlice slice) {
            return slice.getSpace().distance(start.getSpace());
        }

        @Override
        protected String getYLabel() {
            return "distance";
        }

        @Override
        protected double getYValue(Optional<DistanceTimeSpeed> motion) {
            return motion.get().getDistance();
        }
    }



    private class VelocityChart extends AccelChart {


        public VelocityChart(DistancePlot predicted, CarPath actual) {
            super(predicted, actual);
        }

        @Override
        protected String getYLabel() {
            return "velocity";
        }

        @Override
        protected double getYValue(Optional<DistanceTimeSpeed> motion) {
            return motion.get().getSpeed();
        }

        @Override
        protected double getYValue(CarSlice slice) {
            return slice.getVelocity().magnitude();
        }
    }

}
