package tarehart.rlbot.tuning;

import com.google.gson.Gson;
import tarehart.rlbot.carpredict.CarPath;
import tarehart.rlbot.carpredict.CarSlice;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.time.GameTime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CarRecorder {

    public static final String DIRECTORY = "carpath";

    // This is going to be an actual ballpath, not predicted.
    private static CarPath carPath;
    private static GameTime endTime;
    private static Gson gson = new Gson();

    public static void startRecording(CarSlice startPoint, GameTime endTime) {

        if (carPath == null) {
            carPath = new CarPath(startPoint);
            CarRecorder.endTime = endTime;
        }
    }

    public static void recordPosition(CarSlice carSlice) {
        if (carPath != null) {

            if (carSlice.getTime().isAfter(endTime)) {
                // Write to a file
                Path path = Paths.get("./" + DIRECTORY + "/" + endTime.toMillis() + ".json");
                try {
                    Files.write(path, gson.toJson(carPath).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                carPath = null;
            } else {
                carPath.addSlice(carSlice);
            }
        }
    }

}
