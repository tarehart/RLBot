package tarehart.rlbot.tuning;

import com.google.gson.Gson;
import tarehart.rlbot.Bot;
import tarehart.rlbot.carpredict.AccelerationModel;
import tarehart.rlbot.carpredict.CarPath;
import tarehart.rlbot.carpredict.CarSlice;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.CarSpin;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.time.Duration;

import java.io.InputStream;
import java.util.Scanner;

public class CarPathHelper {

    public CarPath readRecording(String filename) {
        InputStream in = getClass().getResourceAsStream("/carrecordings/" + filename);
        Scanner s = new Scanner(in).useDelimiter("\\A");
        String content = s.hasNext() ? s.next() : "";

        Gson gson = new Gson();
        return gson.fromJson(content, CarPath.class);
    }

    public DistancePlot makePrediction(CarPath actual, boolean hasBoost) {
        Duration duration = Duration.Companion.between(actual.getSlices().get(0).getTime(), actual.getSlices().get(actual.getSlices().size() - 1).getTime());
        CarData carData = getCarDataFromSlice(actual.getSlices().get(0));
        return AccelerationModel.INSTANCE.simulateAcceleration(carData, duration, hasBoost ? 100 : 0, 0);
    }

    public CarData getCarDataFromSlice(CarSlice carSlice) {
        return new CarData(
                carSlice.getSpace(),
                carSlice.getVelocity(),
                carSlice.getOrientation(),
                new CarSpin(0, 0, 0),
                100,
                false,
                Bot.Team.BLUE,
                0,
                carSlice.getTime(),
                0,
                true,
                false,
                "");
    }

}
