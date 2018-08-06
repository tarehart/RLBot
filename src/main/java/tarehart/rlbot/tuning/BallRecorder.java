package tarehart.rlbot.tuning;

import com.google.gson.Gson;
import tarehart.rlbot.math.BallSlice;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.time.GameTime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BallRecorder {

    public static final String DIRECTORY = "ballpath";

    // This is going to be an actual ballpath, not predicted.
    private static BallPath ballPath;
    private static GameTime endTime;
    private static Gson gson = new Gson();

    public static void startRecording(BallSlice startPoint, GameTime endTime) {

        if (ballPath == null) {
            ballPath = new BallPath(startPoint);
            BallRecorder.endTime = endTime;
        }
    }

    public static void recordPosition(BallSlice ballPosition) {
        if (ballPath != null) {

            if (ballPosition.getTime().isAfter(endTime)) {
                // Write to a file
                Path path = Paths.get("./" + DIRECTORY + "/" + endTime.toMillis() + ".json");
                try {
                    Files.write(path, gson.toJson(ballPath).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ballPath = null;
            } else {
                ballPath.addSlice(ballPosition);
            }
        }
    }

}
