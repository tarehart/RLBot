package tarehart.rlbot.tuning;

import org.junit.Assert;
import org.junit.Test;
import tarehart.rlbot.carpredict.CarPath;
import tarehart.rlbot.carpredict.CarSlice;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

public class AccelerationPredictedVsActualTest {

    private CarPathHelper carPathHelper = new CarPathHelper();

    @Test
    public void fullBoost() throws UnsupportedEncodingException {
        testFile("fullBoostAccel.json", true);
    }

    @Test
    public void zeroBoost() throws UnsupportedEncodingException {
        testFile("zeroBoostAccel.json", false);
    }


    private void testFile(String filename, boolean hasBoost) throws UnsupportedEncodingException {

        CarPath actualPath = carPathHelper.readRecording(filename);
        DistancePlot predictedAccel = carPathHelper.makePrediction(actualPath, hasBoost);
        // (-73.29997, 65.447556, 4.5342107) after first time step

        List<CarSlice> actual = actualPath.getSlices();
        GameTime startTime = actual.get(0).time;

        for (CarSlice slice : actual) {
            Optional<DistanceTimeSpeed> predicted = predictedAccel.getMotionAfterDuration(Duration.Companion.between(startTime, slice.time));
            Assert.assertTrue(predicted.isPresent());
            Assert.assertEquals(predicted.get().getSpeed(), slice.getVelocity().magnitude(), 1);
        }
    }





}
